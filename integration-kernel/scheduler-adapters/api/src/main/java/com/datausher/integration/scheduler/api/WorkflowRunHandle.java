package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record WorkflowRunHandle(
        String adapterId,
        String bindingId,
        String workflowId,
        String idempotencyKey,
        String externalRunId
) {
    public WorkflowRunHandle {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workflowId = IntegrationIdentifiers.normalize(workflowId, "workflowId");
        idempotencyKey = IntegrationIdentifiers.requireText(idempotencyKey, "idempotencyKey");
        externalRunId = IntegrationIdentifiers.requireText(externalRunId, "externalRunId");
    }
}
