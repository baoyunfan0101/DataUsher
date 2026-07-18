package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record NotificationEnvelope(
        NotificationDispatchId dispatchId,
        String idempotencyKey,
        NotificationRecipient recipient,
        NotificationChannel channel,
        NotificationContent content,
        RequestContext requestContext
) {
    public NotificationEnvelope {
        dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        channel = Objects.requireNonNull(channel, "channel must not be null");
        content = Objects.requireNonNull(content, "content must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
