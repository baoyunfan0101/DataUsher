package com.datausher.ai.runtime.api;

import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.ai.tool.api.AiToolResult;
import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AiToolInvocation(
        AiToolInvocationId invocationId,
        Optional<AiConversationId> conversationId,
        AiToolRef toolRef,
        Map<String, ExecutionValue> arguments,
        AiToolInvocationStatus status,
        Optional<AiToolResult> result,
        String idempotencyKey,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public AiToolInvocation {
        invocationId = Objects.requireNonNull(
                invocationId, "invocationId must not be null");
        conversationId = conversationId == null ? Optional.empty() : conversationId;
        toolRef = Objects.requireNonNull(toolRef, "toolRef must not be null");
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        status = Objects.requireNonNull(status, "status must not be null");
        result = result == null ? Optional.empty() : result;
        idempotencyKey = AiRuntimeValues.text(idempotencyKey, "idempotencyKey");
        attributes = AiRuntimeValues.attributes(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt) || revision < 1) {
            throw new IllegalArgumentException("tool invocation audit fields are invalid");
        }
    }

    public boolean terminal() {
        return status == AiToolInvocationStatus.SUCCEEDED
                || status == AiToolInvocationStatus.FAILED
                || status == AiToolInvocationStatus.DENIED
                || status == AiToolInvocationStatus.CANCELLED;
    }
}
