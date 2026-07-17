package com.datausher.platform.shared.context;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestContextTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void requiresExplicitActorAndRequestTime() {
        assertThrows(NullPointerException.class, () ->
                new RequestContext("request-1", null, NOW, Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                new RequestContext("request-1", ActorContext.SYSTEM, null, Map.of())
        );
    }

    @Test
    void createsExplicitSystemContext() {
        RequestContext context = RequestContext.system("request-1", NOW);

        assertEquals("request-1", context.requestId());
        assertSame(ActorContext.SYSTEM, context.actor());
        assertEquals(NOW, context.requestTime());
    }

    @Test
    void snapshotsContextAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tenant", "tenant-1");

        RequestContext context = new RequestContext(
                "request-1",
                ActorContext.SYSTEM,
                NOW,
                attributes
        );
        attributes.clear();

        assertEquals(Map.of("tenant", "tenant-1"), context.attributes());
        assertThrows(UnsupportedOperationException.class, () -> context.attributes().clear());
    }
}
