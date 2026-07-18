package com.datausher.platform.notification.api;

import java.util.Map;
import java.util.Objects;

public record NotificationChannelResult(
        String providerReference,
        Map<String, String> attributes
) {
    public NotificationChannelResult {
        providerReference = Objects.requireNonNull(
                providerReference, "providerReference must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (providerReference.isEmpty()) {
            throw new IllegalArgumentException("providerReference must not be blank");
        }
    }
}
