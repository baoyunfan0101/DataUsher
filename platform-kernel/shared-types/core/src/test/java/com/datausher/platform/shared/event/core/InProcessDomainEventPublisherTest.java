package com.datausher.platform.shared.event.core;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.BaseDomainEvent;
import com.datausher.platform.shared.event.DomainEventSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InProcessDomainEventPublisherTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void attemptsEverySubscriberAndAggregatesFailures() {
        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher();
        AtomicInteger deliveries = new AtomicInteger();
        DomainEventSubscriber failing = event -> {
            throw new IllegalStateException("delivery failed");
        };

        assertTrue(publisher.subscribe(failing));
        assertFalse(publisher.subscribe(failing));
        publisher.subscribe(event -> deliveries.incrementAndGet());

        DomainEventDeliveryException failure = assertThrows(
                DomainEventDeliveryException.class,
                () -> publisher.publish(event())
        );

        assertEquals(1, deliveries.get());
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(publisher.unsubscribe(failing));
        publisher.publish(event());
        assertEquals(2, deliveries.get());
    }

    @Test
    void publishesCollectionsInIterationOrder() {
        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher();
        List<String> delivered = new ArrayList<>();
        publisher.subscribe(event -> delivered.add(event.eventId()));

        publisher.publishAll(List.of(
                event("event-1"),
                event("event-2")
        ));

        assertEquals(List.of("event-1", "event-2"), delivered);
    }

    private static BaseDomainEvent event() {
        return event("event-1");
    }

    private static BaseDomainEvent event(String eventId) {
        return new BaseDomainEvent(eventId, "Created", "catalog", NOW, requestContext());
    }

    private static RequestContext requestContext() {
        return RequestContext.system("request-1", NOW);
    }
}
