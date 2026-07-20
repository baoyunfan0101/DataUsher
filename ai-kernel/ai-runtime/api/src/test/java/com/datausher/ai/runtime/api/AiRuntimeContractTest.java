package com.datausher.ai.runtime.api;

import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.integration.llm.api.ChatRole;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRuntimeContractTest {
    @Test
    void requiresConversationMessagesToHaveContent() {
        assertThrows(IllegalArgumentException.class, () -> new AiMessage(
                new AiMessageId("message-1"), new AiConversationId("conversation-1"),
                ChatRole.USER, " ", Optional.empty(), Map.of(), Instant.EPOCH));
    }

    @Test
    void modelsTypedToolInvocationArguments() {
        StartAiToolInvocationRequest request = new StartAiToolInvocationRequest(
                Optional.of(new AiConversationId("conversation-1")),
                new AiToolRef(new AiToolId("catalog.search"), 1),
                Map.of("limit", new ExecutionValue.DecimalValue(10)),
                "invoke-1", Map.of(), context());

        assertEquals(new ExecutionValue.DecimalValue(10), request.arguments().get("limit"));
    }

    private static RequestContext context() {
        return RequestContext.system("request-runtime", Instant.EPOCH);
    }
}
