package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record ComputeJobHandle(String adapterId, String bindingId, String externalJobId) {
    public ComputeJobHandle {
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        externalJobId = IntegrationIdentifiers.requireText(externalJobId, "externalJobId");
    }
}
