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
        Instant deliveredAt,
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
        if (status == NotificationDeliveryStatus.PENDING
                && (!providerReference.isEmpty() || !lastError.isEmpty())) {
            throw new IllegalArgumentException("pending delivery must not have an outcome");
        }
        if (status != NotificationDeliveryStatus.PENDING && (attempts < 1 || lastAttemptedAt == null)) {
            throw new IllegalArgumentException("attempted delivery must include attempt details");
        }
        if ((status == NotificationDeliveryStatus.ACCEPTED || status == NotificationDeliveryStatus.DELIVERED)
                && providerReference.isEmpty()) {
            throw new IllegalArgumentException("accepted delivery must have a providerReference");
        }
        if ((status == NotificationDeliveryStatus.ACCEPTED || status == NotificationDeliveryStatus.DELIVERED)
                && !lastError.isEmpty()) {
            throw new IllegalArgumentException("accepted delivery must not have a lastError");
        }
        if (status == NotificationDeliveryStatus.FAILED && lastError.isEmpty()) {
            throw new IllegalArgumentException("failed delivery must have a lastError");
        }
        if (status == NotificationDeliveryStatus.FAILED && !providerReference.isEmpty()) {
            throw new IllegalArgumentException("failed delivery must not have a providerReference");
        }
        if ((status == NotificationDeliveryStatus.DELIVERED) != (deliveredAt != null)) {
            throw new IllegalArgumentException("deliveredAt must be present only for delivered status");
        }
    }
}
