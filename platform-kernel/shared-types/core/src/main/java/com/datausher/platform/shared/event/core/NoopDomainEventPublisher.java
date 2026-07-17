package com.datausher.platform.shared.event.core;

import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;

import java.util.Objects;

public final class NoopDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
    }
}
