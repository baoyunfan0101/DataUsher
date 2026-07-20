package com.datausher.ai.context.core;

import com.datausher.ai.context.api.AiContextAssemblyRequest;
import com.datausher.ai.context.api.AiContextItem;
import com.datausher.ai.context.api.AiContextQuery;
import com.datausher.ai.context.api.AiContextResult;
import com.datausher.ai.context.api.AiContextSourceRef;
import com.datausher.ai.context.api.AiContextSourceType;
import com.datausher.governance.access.api.AccessDecision;
import com.datausher.governance.access.api.AccessDecisionCode;
import com.datausher.governance.access.api.AccessDecisionService;
import com.datausher.governance.access.api.AccessRequest;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAiContextQueryServiceTest {
    @Test
    void filtersContextItemsByPermissionAndResource() {
        ResourceRef allowed = ResourceRef.global("table", "orders");
        ResourceRef denied = ResourceRef.global("table", "customers");
        DefaultAiContextQueryService service = new DefaultAiContextQueryService(
                List.of(provider(item("Orders", allowed, 90), item("Customers", denied, 80))),
                request -> decision(request.resource().equals(allowed)));

        AiContextResult result = service.search(new AiContextQuery(
                "table", Set.of(subject()), Set.of(), Set.of(), 10, Map.of(), context()));

        assertEquals(List.of("Orders"), result.items().stream()
                .map(AiContextItem::title).toList());
    }

    @Test
    void assemblerHonorsTokenBudget() {
        DefaultAiContextAssembler assembler = new DefaultAiContextAssembler();
        var context = assembler.assemble(new AiContextAssemblyRequest(
                List.of(item("Long", ResourceRef.global("table", "orders"), 90)),
                1, Map.of(), context()));

        assertTrue(context.truncated());
        assertEquals(0, context.sections().size());
    }

    private static AiContextProvider provider(AiContextItem... items) {
        return new AiContextProvider() {
            @Override
            public AiContextSourceType sourceType() {
                return AiContextSourceType.METADATA;
            }

            @Override
            public AiContextResult search(AiContextQuery query) {
                return new AiContextResult(List.of(items), false, Map.of());
            }
        };
    }

    private static AiContextItem item(String title, ResourceRef resource, int priority) {
        return new AiContextItem(new AiContextSourceRef(
                AiContextSourceType.METADATA, resource.resourceId(), Optional.of(resource),
                Map.of()), title, title + " context content", priority, Map.of());
    }

    private static AccessDecision decision(boolean allowed) {
        return new AccessDecision(
                allowed, allowed ? AccessDecisionCode.ALLOWED : AccessDecisionCode.DENIED_BY_POLICY,
                allowed ? "allowed" : "denied", null, Instant.EPOCH);
    }

    private static SubjectRef subject() {
        return new SubjectRef(SubjectType.USER, "alice");
    }

    private static RequestContext context() {
        return RequestContext.system("request-ai-context", Instant.EPOCH);
    }
}
