package com.datausher.ai.runtime.api;

import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record StartAiToolInvocationRequest(
        Optional<AiConversationId> conversationId,
        AiToolRef toolRef,
        Map<String, ExecutionValue> arguments,
        String idempotencyKey,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public StartAiToolInvocationRequest {
        conversationId = conversationId == null ? Optional.empty() : conversationId;
        toolRef = Objects.requireNonNull(toolRef, "toolRef must not be null");
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        idempotencyKey = AiRuntimeValues.text(idempotencyKey, "idempotencyKey");
        attributes = AiRuntimeValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
