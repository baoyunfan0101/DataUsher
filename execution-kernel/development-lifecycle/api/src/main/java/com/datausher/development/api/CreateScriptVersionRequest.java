package com.datausher.development.api;

import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record CreateScriptVersionRequest(
        ScriptId scriptId,
        long expectedRevision,
        ExecutionSpecification executionSpecification,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateScriptVersionRequest {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        executionSpecification = Objects.requireNonNull(
                executionSpecification, "executionSpecification must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
