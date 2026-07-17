package com.datausher.platform.shared.event.core;

public final class DomainEventDeliveryException extends RuntimeException {
    public DomainEventDeliveryException(String eventId) {
        super("one or more subscribers failed for event: " + eventId);
    }
}
