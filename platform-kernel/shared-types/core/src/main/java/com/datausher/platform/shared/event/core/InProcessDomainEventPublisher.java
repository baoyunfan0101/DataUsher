package com.datausher.platform.shared.event.core;

import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.event.DomainEventSubscriber;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InProcessDomainEventPublisher implements DomainEventPublisher {
    private final CopyOnWriteArrayList<DomainEventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    public boolean subscribe(DomainEventSubscriber subscriber) {
        return subscribers.addIfAbsent(Objects.requireNonNull(subscriber, "subscriber must not be null"));
    }

    public boolean unsubscribe(DomainEventSubscriber subscriber) {
        return subscribers.remove(Objects.requireNonNull(subscriber, "subscriber must not be null"));
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        DomainEventDeliveryException deliveryFailure = null;
        for (DomainEventSubscriber subscriber : subscribers) {
            try {
                subscriber.handle(event);
            } catch (RuntimeException subscriberFailure) {
                if (deliveryFailure == null) {
                    deliveryFailure = new DomainEventDeliveryException(event.eventId());
                }
                deliveryFailure.addSuppressed(subscriberFailure);
            }
        }
        if (deliveryFailure != null) {
            throw deliveryFailure;
        }
    }
}
