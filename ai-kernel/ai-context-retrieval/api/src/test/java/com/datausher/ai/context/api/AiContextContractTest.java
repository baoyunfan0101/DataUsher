package com.datausher.ai.context.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiContextContractTest {
    @Test
    void keepsContextSourceTypesOpenForExtension() {
        assertEquals("feature-store", new AiContextSourceType("feature-store").value());
    }

    @Test
    void requiresSubjectsForPermissionAwareRetrieval() {
        assertThrows(IllegalArgumentException.class, () -> new AiContextQuery(
                "orders", Set.of(), Set.of(), Set.of(), 10, Map.of(), context()));
    }

    @Test
    void preservesSourceResourceAndAttributes() {
        ResourceRef resource = ResourceRef.global("table", "orders");
        AiContextItem item = new AiContextItem(
                new AiContextSourceRef(
                        AiContextSourceType.METADATA, "orders", Optional.of(resource),
                        Map.of("schema", "public")),
                "Orders", "Orders table", 50, Map.of("domain", "sales"));

        assertEquals(resource, item.source().resource().orElseThrow());
        assertEquals("sales", item.attributes().get("domain"));
    }

    private static RequestContext context() {
        return RequestContext.system("request-ai-context", Instant.EPOCH);
    }

    static SubjectRef subject() {
        return new SubjectRef(SubjectType.USER, "alice");
    }
}
