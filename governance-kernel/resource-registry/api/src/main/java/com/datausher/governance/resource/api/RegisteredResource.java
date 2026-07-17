package com.datausher.governance.resource.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record RegisteredResource(
        ResourceRef ref,
        String displayName,
        ResourceLifecycle lifecycle,
        Instant registeredAt,
        String registeredBy,
        Map<String, String> attributes
) {
    public RegisteredResource {
        ref = Objects.requireNonNull(ref, "ref must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        registeredAt = Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        registeredBy = Objects.requireNonNull(registeredBy, "registeredBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty() || registeredBy.isEmpty()) {
            throw new IllegalArgumentException("displayName and registeredBy must not be blank");
        }
    }

    public RegisteredResource withLifecycle(ResourceLifecycle nextLifecycle) {
        return new RegisteredResource(ref, displayName, nextLifecycle, registeredAt, registeredBy, attributes);
    }
}
