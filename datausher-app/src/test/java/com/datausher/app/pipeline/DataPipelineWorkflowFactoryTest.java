package com.datausher.app.pipeline;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.datasource.api.DatasourceOperations;
import com.datausher.integration.datasource.clickhouse.ClickHouseDatasourceConnector;
import com.datausher.integration.datasource.hive.HiveDatasourceConnector;
import com.datausher.integration.datasource.mysql.MySqlDatasourceConnector;
import com.datausher.integration.runtime.api.IntegrationValue;
import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.TaskDependency;
import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowRuntimeBinding;
import com.datausher.workflow.api.WorkflowRuntimeType;
import com.datausher.workflow.api.WorkflowSchedule;
import com.datausher.workflow.api.WorkflowScheduleId;
import com.datausher.workflow.api.WorkflowScheduleStatus;
import com.datausher.workflow.api.WorkflowScheduleType;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataPipelineWorkflowFactoryTest {
    @Test
    void composesUserDefinedPipelineAcrossRequiredEngines() {
        var workflow = DataPipelineWorkflowFactory.create(spec());

        assertEquals(List.of(
                "load-user-sql",
                "publish-user-serving",
                "validate-dashboard-source"
        ), workflow.tasks().stream().map(WorkflowTaskDefinition::taskKey).toList());
        assertEquals(List.of(
                "load-user-sql>publish-user-serving",
                "publish-user-serving>validate-dashboard-source"
        ), workflow.dependencies().stream()
                .map(dependency -> dependency.upstreamTaskKey() + ">" + dependency.downstreamTaskKey())
                .toList());
        assertEquals(List.of(TaskDependencyCondition.ON_SUCCESS, TaskDependencyCondition.ON_SUCCESS),
                workflow.dependencies().stream().map(TaskDependency::condition).toList());
        assertEquals(Map.of(
                "pipelineId", "user-defined-pipeline",
                "source", MySqlDatasourceConnector.ADAPTER_ID,
                "processing", "spark-sql",
                "offlineStorage", HiveDatasourceConnector.ADAPTER_ID,
                "servingStore", ClickHouseDatasourceConnector.ADAPTER_ID,
                "owner", "analytics"
        ), workflow.attributes());
    }

    @Test
    void preservesUserAuthoredSparkSqlPayloads() {
        var workflow = DataPipelineWorkflowFactory.create(spec());

        var loadWorkload = workflow.tasks().get(0).executionSpecification().workload();
        var publishWorkload = workflow.tasks().get(1).executionSpecification().workload();
        assertEquals(DataPipelineEngineBoundary.SPARK_SQL, loadWorkload.type());
        assertEquals("insert overwrite table user_hive.user_ods select * from mysql_source.user_table",
                loadWorkload.payload());
        assertEquals("insert into clickhouse_serving.user_dashboard select * from user_hive.user_ads",
                publishWorkload.payload());
        assertEquals(Map.of(
                "sourceBinding", new ExecutionValue.TextValue("prod-mysql"),
                "offlineBinding", new ExecutionValue.TextValue("warehouse-hive")
        ), loadWorkload.parameters());
        assertEquals("spark-prod", loadWorkload.options().get("computeAdapterId"));
    }

    @Test
    void preservesUserSuppliedDailySchedulerBoundary() {
        var workflow = DataPipelineWorkflowFactory.create(spec());
        var schedule = workflow.schedules().getFirst();

        assertEquals(WorkflowRuntimeType.SCHEDULER_MANAGED, workflow.runtimeBinding().runtimeType());
        assertEquals(Optional.of("airflow"), workflow.runtimeBinding().adapterId());
        assertEquals(Optional.of("daily-scheduler"), workflow.runtimeBinding().bindingId());
        assertEquals(new WorkflowScheduleId("user-defined-pipeline-daily"), schedule.scheduleId());
        assertEquals(WorkflowScheduleType.CRON, schedule.type());
        assertEquals("0 4 * * *", schedule.expression());
        assertEquals(ZoneId.of("Asia/Shanghai"), schedule.zoneId());
        assertEquals(WorkflowScheduleStatus.ENABLED, schedule.status());
        assertEquals(Map.of("frequency", "daily"), schedule.options());
    }

    @Test
    void supportsClickHouseServingValidationAsUserDefinedAdapterTask() {
        var workflow = DataPipelineWorkflowFactory.create(spec());
        var validateAction = (AdapterWorkflowTaskAction) workflow.tasks().get(2).action();

        assertEquals(ClickHouseDatasourceConnector.ADAPTER_ID, validateAction.adapterId());
        assertEquals("dashboard-clickhouse", validateAction.bindingId());
        assertEquals(DatasourceOperations.DISCOVER, validateAction.operation());
        assertEquals(Map.of(
                "namespace", new IntegrationValue.TextValue("serving"),
                "object", new IntegrationValue.TextValue("user_dashboard")
        ), validateAction.parameters());
        assertEquals("user-defined-pipeline-dashboard-source", validateAction.idempotencyKey());
    }

    @Test
    void rejectsCallerOverridesOfManagedBoundaryAttributes() {
        var tasks = List.of(sparkTask(
                "load-user-sql",
                "insert overwrite table user_hive.user_ods select * from mysql_source.user_table"));

        assertThrows(IllegalArgumentException.class, () -> new DataPipelineWorkflowSpec(
                "user-defined-pipeline",
                DataPipelineEngineBoundary.mysqlSparkHiveClickHouse(),
                tasks,
                List.of(),
                List.of(),
                WorkflowRuntimeBinding.PLATFORM_MANAGED,
                Map.of("source", "other")
        ));
    }

    private static DataPipelineWorkflowSpec spec() {
        WorkflowTaskDefinition load = sparkTask(
                "load-user-sql",
                "insert overwrite table user_hive.user_ods select * from mysql_source.user_table");
        WorkflowTaskDefinition publish = sparkTask(
                "publish-user-serving",
                "insert into clickhouse_serving.user_dashboard select * from user_hive.user_ads");
        WorkflowTaskDefinition validate = DataPipelineWorkflowFactory.adapterTask(
                "validate-dashboard-source",
                "Validate dashboard source",
                ClickHouseDatasourceConnector.ADAPTER_ID,
                "dashboard-clickhouse",
                DatasourceOperations.DISCOVER,
                Map.of(
                        "namespace", new IntegrationValue.TextValue("serving"),
                        "object", new IntegrationValue.TextValue("user_dashboard")
                ),
                "user-defined-pipeline-dashboard-source",
                retryPolicy(),
                Duration.ofMinutes(10),
                Map.of("step", "validate", "servingStore", ClickHouseDatasourceConnector.ADAPTER_ID)
        );
        return new DataPipelineWorkflowSpec(
                "user-defined-pipeline",
                DataPipelineEngineBoundary.mysqlSparkHiveClickHouse(),
                List.of(load, publish, validate),
                List.of(
                        new TaskDependency("load-user-sql", "publish-user-serving",
                                TaskDependencyCondition.ON_SUCCESS),
                        new TaskDependency("publish-user-serving", "validate-dashboard-source",
                                TaskDependencyCondition.ON_SUCCESS)
                ),
                List.of(new WorkflowSchedule(
                        new WorkflowScheduleId("user-defined-pipeline-daily"),
                        WorkflowScheduleType.CRON,
                        "0 4 * * *",
                        ZoneId.of("Asia/Shanghai"),
                        WorkflowScheduleStatus.ENABLED,
                        Map.of("frequency", "daily")
                )),
                WorkflowRuntimeBinding.schedulerManaged(
                        "airflow", "daily-scheduler", Map.of("frequency", "daily")),
                Map.of("owner", "analytics")
        );
    }

    private static WorkflowTaskDefinition sparkTask(String taskKey, String sqlPayload) {
        return DataPipelineWorkflowFactory.sparkSqlTask(
                taskKey,
                "Run user Spark SQL",
                "spark-prod",
                new ExecutionQueueId("spark-batch"),
                new ExecutionAccountId("analytics"),
                sqlPayload,
                Map.of(
                        "sourceBinding", new ExecutionValue.TextValue("prod-mysql"),
                        "offlineBinding", new ExecutionValue.TextValue("warehouse-hive")
                ),
                Map.of("resultMode", "reference"),
                ExecutionResultMode.REFERENCE,
                100,
                retryPolicy(),
                Duration.ofHours(2),
                Map.of("step", taskKey)
        );
    }

    private static TaskRetryPolicy retryPolicy() {
        return new TaskRetryPolicy(2, Duration.ofMinutes(5), java.util.Set.of());
    }
}
