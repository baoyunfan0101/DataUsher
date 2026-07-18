package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;
import java.util.Objects;

public record ConfirmNotificationDeliveryRequest(
        NotificationDispatchId dispatchId,
        NotificationChannel channel,
        String providerReference,
        Instant deliveredAt,
        RequestContext requestContext
) {
    public ConfirmNotificationDeliveryRequest {
        dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        channel = Objects.requireNonNull(channel, "channel must not be null");
        providerReference = Objects.requireNonNull(
                providerReference, "providerReference must not be null").trim();
        deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (providerReference.isEmpty()) {
            throw new IllegalArgumentException("providerReference must not be blank");
        }
    }
}
