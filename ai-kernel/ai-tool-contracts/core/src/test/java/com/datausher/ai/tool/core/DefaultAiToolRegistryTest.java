package com.datausher.ai.tool.core;

import com.datausher.ai.tool.api.AiToolCatalogQuery;
import com.datausher.ai.tool.api.AiToolDefinition;
import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolParameter;
import com.datausher.ai.tool.api.AiToolParameterType;
import com.datausher.ai.tool.api.AiToolPermissionPolicy;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.ai.tool.api.AiToolSchema;
import com.datausher.ai.tool.api.AiToolStatus;
import com.datausher.ai.tool.api.ChangeAiToolStatusRequest;
import com.datausher.ai.tool.api.RegisterAiToolRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.page.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAiToolRegistryTest {
    @Test
    void registersToolsAndChangesStatusWithRevisionChecks() {
        DefaultAiToolRegistry registry = new DefaultAiToolRegistry(new InMemoryAiToolStore());
        AiToolDefinition created = registry.register(new RegisterAiToolRequest(
                schema(AiToolStatus.ACTIVE, 1), AiToolPermissionPolicy.none(), context()));

        AiToolDefinition disabled = registry.changeStatus(new ChangeAiToolStatusRequest(
                created.schema().ref(), AiToolStatus.DISABLED,
                created.schema().revision(), context()));

        assertEquals(AiToolStatus.DISABLED, disabled.schema().status());
        assertEquals(2, disabled.schema().revision());
        assertEquals(1, registry.search(new AiToolCatalogQuery(
                java.util.Set.of(AiToolStatus.DISABLED)), PageRequest.firstPage()).total());
    }

    private static AiToolSchema schema(AiToolStatus status, long revision) {
        return new AiToolSchema(
                new AiToolRef(new AiToolId("catalog.search"), 1),
                "Catalog search", "", List.of(new AiToolParameter(
                "query", AiToolParameterType.STRING, true, "", Map.of())),
                status, Map.of(), revision);
    }

    private static RequestContext context() {
        return RequestContext.system("request-ai-tool", Instant.EPOCH);
    }
}
