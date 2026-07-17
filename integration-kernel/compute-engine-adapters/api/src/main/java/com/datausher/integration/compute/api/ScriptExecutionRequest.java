package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Map;

public record ScriptExecutionRequest(
        String bindingId,
        String language,
        String script,
        Map<String, String> options
) {
    public ScriptExecutionRequest {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        language = IntegrationIdentifiers.normalize(language, "language");
        script = IntegrationIdentifiers.requireText(script, "script");
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
