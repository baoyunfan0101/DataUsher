package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record NotificationDeliveryFailedEvent(
        String eventId,
        Instant occurredAt,
        RequestContext requestContext,
        NotificationDispatchId dispatchId,
        NotificationDelivery delivery
) implements DomainEvent {
    public NotificationDeliveryFailedEvent {
        eventId = normalize(eventId);
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        delivery = Objects.requireNonNull(delivery, "delivery must not be null");
        if (delivery.status() != NotificationDeliveryStatus.FAILED) {
            throw new IllegalArgumentException("delivery must be failed");
        }
    }

    @Override
    public String eventType() {
        return NotificationEvents.DELIVERY_FAILED;
    }

    @Override
    public String sourceModule() {
        return "notification";
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "eventId must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return normalized;
    }
}
