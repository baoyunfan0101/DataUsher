package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SendNotificationRequest(
        NotificationTemplateKey templateKey,
        List<NotificationRecipient> recipients,
        Map<String, String> parameters,
        String idempotencyKey,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public SendNotificationRequest {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        recipients = List.copyOf(Objects.requireNonNull(recipients, "recipients must not be null"));
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (recipients.isEmpty() || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("recipients and idempotencyKey must not be empty");
        }
        if (recipients.stream().map(NotificationRecipient::canonicalValue).distinct().count() != recipients.size()) {
            throw new IllegalArgumentException("recipients must be unique");
        }
    }
}
