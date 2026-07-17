package com.datausher.platform.shared.event;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainEventTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void requiresExplicitTimeAndRequestContext() {
        RequestContext requestContext = RequestContext.system("request-1", NOW);

        assertThrows(NullPointerException.class, () ->
                new BaseDomainEvent("event-1", "Created", "catalog", null, requestContext)
        );
        assertThrows(NullPointerException.class, () ->
                new BaseDomainEvent("event-1", "Created", "catalog", NOW, null)
        );
    }

    @Test
    void supportsTypedEventDataOwnedByThePublishingModule() {
        CatalogCreated event = new CatalogCreated(
                "event-1",
                "CatalogCreated",
                "catalog",
                NOW,
                RequestContext.system("request-1", NOW),
                "catalog-1"
        );

        assertEquals("catalog-1", event.catalogId());
    }

    private record CatalogCreated(
            String eventId,
            String eventType,
            String sourceModule,
            Instant occurredAt,
            RequestContext requestContext,
            String catalogId
    ) implements DomainEvent {
    }
}
