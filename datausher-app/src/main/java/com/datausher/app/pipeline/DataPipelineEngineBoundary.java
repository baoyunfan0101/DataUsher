package com.datausher.app.pipeline;

import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.integration.datasource.clickhouse.ClickHouseDatasourceConnector;
import com.datausher.integration.datasource.hive.HiveDatasourceConnector;
import com.datausher.integration.datasource.mysql.MySqlDatasourceConnector;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;
import java.util.Objects;

public record DataPipelineEngineBoundary(
        String sourceAdapterId,
        ExecutionWorkloadType processingWorkloadType,
        String offlineStorageAdapterId,
        String servingAdapterId
) {
    public static final ExecutionWorkloadType SPARK_SQL = new ExecutionWorkloadType("spark-sql");

    public DataPipelineEngineBoundary {
        sourceAdapterId = IntegrationIdentifiers.normalize(sourceAdapterId, "sourceAdapterId");
        processingWorkloadType = Objects.requireNonNull(
                processingWorkloadType, "processingWorkloadType must not be null");
        offlineStorageAdapterId = IntegrationIdentifiers.normalize(
                offlineStorageAdapterId, "offlineStorageAdapterId");
        servingAdapterId = IntegrationIdentifiers.normalize(servingAdapterId, "servingAdapterId");
    }

    public static DataPipelineEngineBoundary mysqlSparkHiveClickHouse() {
        return new DataPipelineEngineBoundary(
                MySqlDatasourceConnector.ADAPTER_ID,
                SPARK_SQL,
                HiveDatasourceConnector.ADAPTER_ID,
                ClickHouseDatasourceConnector.ADAPTER_ID
        );
    }

    public Map<String, String> workflowAttributes() {
        return Map.of(
                "source", sourceAdapterId,
                "processing", processingWorkloadType.value(),
                "offlineStorage", offlineStorageAdapterId,
                "servingStore", servingAdapterId
        );
    }
}
