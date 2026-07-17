package com.datausher.platform.shared.event;

import java.util.Collection;
import java.util.Objects;

public interface DomainEventPublisher {
    void publish(DomainEvent event);

    default void publishAll(Collection<? extends DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null").forEach(this::publish);
    }
}
