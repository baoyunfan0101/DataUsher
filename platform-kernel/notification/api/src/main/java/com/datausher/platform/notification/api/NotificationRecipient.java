package com.datausher.platform.notification.api;

import java.util.Map;
import java.util.Objects;

public record NotificationRecipient(
        NotificationRecipientType type,
        String recipientId,
        Map<String, String> attributes
) {
    public NotificationRecipient {
        type = Objects.requireNonNull(type, "type must not be null");
        recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (recipientId.isEmpty()) {
            throw new IllegalArgumentException("recipientId must not be blank");
        }
    }

    public String canonicalValue() {
        return type.value() + ":" + recipientId;
    }
}
