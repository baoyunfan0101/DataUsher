package com.datausher.platform.notification.api;

import java.time.Instant;
import java.util.Objects;

public record NotificationDelivery(
        NotificationRecipient recipient,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        int attempts,
        Instant lastAttemptedAt,
        String providerReference,
        String lastError
) {
    public NotificationDelivery {
        recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        channel = Objects.requireNonNull(channel, "channel must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must be greater than or equal to 0");
        }
        providerReference = providerReference == null ? "" : providerReference.trim();
        lastError = lastError == null ? "" : lastError.trim();
        if (status == NotificationDeliveryStatus.PENDING && (attempts != 0 || lastAttemptedAt != null)) {
            throw new IllegalArgumentException("pending delivery must not have attempts");
        }
        if (status != NotificationDeliveryStatus.PENDING && (attempts < 1 || lastAttemptedAt == null)) {
            throw new IllegalArgumentException("attempted delivery must include attempt details");
        }
        if (status == NotificationDeliveryStatus.SUCCEEDED && providerReference.isEmpty()) {
            throw new IllegalArgumentException("successful delivery must have a providerReference");
        }
        if (status == NotificationDeliveryStatus.SUCCEEDED && !lastError.isEmpty()) {
            throw new IllegalArgumentException("successful delivery must not have a lastError");
        }
        if (status == NotificationDeliveryStatus.FAILED && lastError.isEmpty()) {
            throw new IllegalArgumentException("failed delivery must have a lastError");
        }
    }
}
