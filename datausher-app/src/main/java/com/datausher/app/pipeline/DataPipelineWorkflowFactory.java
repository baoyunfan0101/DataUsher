package com.datausher.app.pipeline;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.integration.runtime.api.AdapterOperation;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;
import com.datausher.workflow.api.AdapterWorkflowTaskAction;
import com.datausher.workflow.api.ExecutionWorkflowTaskAction;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersionSpec;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DataPipelineWorkflowFactory {
    private static final Set<String> MANAGED_ATTRIBUTE_KEYS = Set.of(
            "pipelineId", "source", "processing", "offlineStorage", "servingStore");

    private DataPipelineWorkflowFactory() {
    }

    public static WorkflowVersionSpec create(DataPipelineWorkflowSpec spec) {
        Objects.requireNonNull(spec, "spec must not be null");
        return new WorkflowVersionSpec(
                spec.tasks(),
                spec.dependencies(),
                spec.schedules(),
                spec.runtimeBinding(),
                workflowAttributes(spec)
        );
    }

    public static WorkflowTaskDefinition sparkSqlTask(
            String taskKey,
            String displayName,
            String sparkAdapterId,
            ExecutionQueueId queueId,
            ExecutionAccountId accountId,
            String sqlPayload,
            Map<String, ExecutionValue> parameters,
            Map<String, String> options,
            ExecutionResultMode resultMode,
            int resultPageSize,
            TaskRetryPolicy retryPolicy,
            Duration timeout,
            Map<String, String> attributes
    ) {
        String adapterId = IntegrationIdentifiers.normalize(sparkAdapterId, "sparkAdapterId");
        Map<String, String> workloadOptions = new LinkedHashMap<>(
                options == null ? Map.of() : options);
        workloadOptions.put("computeAdapterId", adapterId);
        ExecutionWorkload workload = new ExecutionWorkload(
                DataPipelineEngineBoundary.SPARK_SQL,
                sqlPayload,
                parameters,
                workloadOptions
        );
        ExecutionSpecification specification = new ExecutionSpecification(
                queueId,
                accountId,
                workload,
                resultMode,
                resultPageSize
        );
        return new WorkflowTaskDefinition(
                taskKey,
                displayName,
                new ExecutionWorkflowTaskAction(specification),
                retryPolicy,
                timeout,
                attributes
        );
    }

    public static WorkflowTaskDefinition adapterTask(
            String taskKey,
            String displayName,
            String adapterId,
            String bindingId,
            AdapterOperation operation,
            Map<String, IntegrationValue> parameters,
            String idempotencyKey,
            TaskRetryPolicy retryPolicy,
            Duration timeout,
            Map<String, String> attributes
    ) {
        return new WorkflowTaskDefinition(
                taskKey,
                displayName,
                new AdapterWorkflowTaskAction(
                        adapterId,
                        bindingId,
                        operation,
                        parameters,
                        idempotencyKey),
                retryPolicy,
                timeout,
                attributes
        );
    }

    static void requireNoManagedAttributes(Map<String, String> attributes) {
        for (String key : attributes.keySet()) {
            if (MANAGED_ATTRIBUTE_KEYS.contains(key)) {
                throw new IllegalArgumentException("attribute is managed by pipeline composition: " + key);
            }
        }
    }

    private static Map<String, String> workflowAttributes(DataPipelineWorkflowSpec spec) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("pipelineId", spec.pipelineId());
        attributes.putAll(spec.engineBoundary().workflowAttributes());
        attributes.putAll(spec.attributes());
        return Map.copyOf(attributes);
    }
}
