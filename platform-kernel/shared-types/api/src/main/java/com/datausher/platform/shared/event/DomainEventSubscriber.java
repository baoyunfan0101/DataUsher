package com.datausher.platform.shared.event;

@FunctionalInterface
public interface DomainEventSubscriber {
    void handle(DomainEvent event);
}
