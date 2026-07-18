package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PublishNotificationTemplateRequest(
        NotificationTemplateKey templateKey,
        String displayName,
        List<NotificationRoute> routes,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public PublishNotificationTemplateRequest {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        routes = List.copyOf(Objects.requireNonNull(routes, "routes must not be null"));
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty() || routes.isEmpty()) {
            throw new IllegalArgumentException("displayName and routes must not be empty");
        }
        if (new HashSet<>(routes.stream().map(NotificationRoute::channel).toList()).size() != routes.size()) {
            throw new IllegalArgumentException("template routes must use unique channels");
        }
    }
}
