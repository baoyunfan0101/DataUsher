package com.datausher.platform.notification.api;

import java.util.Map;
import java.util.Objects;

public record NotificationContent(
        String contentType,
        String subject,
        String body,
        Map<String, String> attributes
) {
    public NotificationContent {
        contentType = Objects.requireNonNull(contentType, "contentType must not be null").trim().toLowerCase();
        subject = subject == null ? "" : subject;
        body = Objects.requireNonNull(body, "body must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (contentType.isEmpty() || body.isEmpty()) {
            throw new IllegalArgumentException("contentType and body must not be empty");
        }
    }
}
