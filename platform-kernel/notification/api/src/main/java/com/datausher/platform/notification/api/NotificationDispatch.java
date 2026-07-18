package com.datausher.platform.notification.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NotificationDispatch(
        NotificationDispatchId dispatchId,
        NotificationTemplateKey templateKey,
        long templateVersion,
        String idempotencyKey,
        NotificationDispatchStatus status,
        List<NotificationDelivery> deliveries,
        Instant createdAt,
        Instant completedAt,
        Map<String, String> attributes
) {
    public NotificationDispatch {
        dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        if (templateVersion < 1) {
            throw new IllegalArgumentException("templateVersion must be greater than or equal to 1");
        }
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        deliveries = List.copyOf(Objects.requireNonNull(deliveries, "deliveries must not be null"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (idempotencyKey.isEmpty() || deliveries.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey and deliveries must not be empty");
        }
        if ((status == NotificationDispatchStatus.PENDING) == (completedAt != null)) {
            throw new IllegalArgumentException("completedAt must be null only while pending");
        }
    }
}
