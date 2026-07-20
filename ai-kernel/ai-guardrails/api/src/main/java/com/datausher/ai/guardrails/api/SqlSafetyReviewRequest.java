package com.datausher.ai.guardrails.api;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record SqlSafetyReviewRequest(
        String statement,
        Map<String, ExecutionValue> parameters,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public SqlSafetyReviewRequest {
        statement = AiGuardrailValues.text(statement, "statement");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        attributes = AiGuardrailValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
