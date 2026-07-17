package com.datausher.platform.shared.event;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainEventTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void requiresExplicitTimeAndRequestContext() {
        RequestContext requestContext = RequestContext.system("request-1", NOW);

        assertThrows(NullPointerException.class, () ->
                new BaseDomainEvent("event-1", "Created", "catalog", null, requestContext, Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                new BaseDomainEvent("event-1", "Created", "catalog", NOW, null, Map.of())
        );
    }
}
