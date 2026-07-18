package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record PublishedWorkflow(
        String adapterId,
        String bindingId,
        String workflowId,
        String idempotencyKey,
        String externalWorkflowId,
        long revision
) {
    public PublishedWorkflow {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workflowId = IntegrationIdentifiers.normalize(workflowId, "workflowId");
        idempotencyKey = IntegrationIdentifiers.requireText(idempotencyKey, "idempotencyKey");
        externalWorkflowId = IntegrationIdentifiers.requireText(
                externalWorkflowId, "externalWorkflowId");
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }
}
