package com.datausher.platform.shared.context;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record RequestContext(
        String requestId,
        ActorContext actor,
        Instant requestTime,
        Map<String, String> attributes
) {
    public RequestContext {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null").trim();
        actor = Objects.requireNonNull(actor, "actor must not be null");
        requestTime = Objects.requireNonNull(requestTime, "requestTime must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (requestId.isEmpty()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
    }

    public static RequestContext system(String requestId, Instant requestTime) {
        return new RequestContext(requestId, ActorContext.SYSTEM, requestTime, Map.of());
    }
}
