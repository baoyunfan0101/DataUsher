package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record WorkflowRunHandle(String adapterId, String bindingId, String externalRunId) {
    public WorkflowRunHandle {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        externalRunId = IntegrationIdentifiers.requireText(externalRunId, "externalRunId");
    }
}
