package com.datausher.platform.shared.event;

import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;

public interface DomainEvent {
    String eventId();

    String eventType();

    String sourceModule();

    Instant occurredAt();

    RequestContext requestContext();
}
