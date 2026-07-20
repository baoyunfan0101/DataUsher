package com.datausher.workflow.api;

import com.datausher.integration.runtime.api.AdapterOperation;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.Objects;

public record AdapterWorkflowTaskAction(
        String adapterId,
        String bindingId,
        AdapterOperation operation,
        Map<String, IntegrationValue> parameters,
        String idempotencyKey
) implements WorkflowTaskAction {
    public AdapterWorkflowTaskAction {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        operation = Objects.requireNonNull(operation, "operation must not be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        parameters.keySet().forEach(key -> IntegrationIdentifiers.normalize(key, "parameter key"));
        idempotencyKey = IntegrationIdentifiers.requireText(idempotencyKey, "idempotencyKey");
    }

    @Override
    public WorkflowTaskType taskType() {
        return WorkflowTaskType.ADAPTER;
    }
}
