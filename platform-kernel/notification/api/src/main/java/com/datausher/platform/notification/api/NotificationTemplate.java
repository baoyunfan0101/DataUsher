package com.datausher.platform.notification.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NotificationTemplate(
        NotificationTemplateKey templateKey,
        long version,
        String displayName,
        List<NotificationRoute> routes,
        Instant publishedAt,
        String publishedBy,
        Map<String, String> attributes
) {
    public NotificationTemplate {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than or equal to 1");
        }
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        routes = List.copyOf(Objects.requireNonNull(routes, "routes must not be null"));
        publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        publishedBy = Objects.requireNonNull(publishedBy, "publishedBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (displayName.isEmpty() || routes.isEmpty() || publishedBy.isEmpty()) {
            throw new IllegalArgumentException("displayName, routes, and publishedBy must not be empty");
        }
    }
}
