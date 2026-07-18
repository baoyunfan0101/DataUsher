package com.datausher.development.api;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record StartDebugRunRequest(
        ScriptId scriptId,
        long scriptVersion,
        String idempotencyKey,
        Map<String, ExecutionValue> parameters,
        RequestContext requestContext
) {
    public StartDebugRunRequest {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (scriptVersion < 1 || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("scriptVersion and idempotencyKey are required");
        }
    }
}
