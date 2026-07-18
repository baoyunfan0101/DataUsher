package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record RetryNotificationRequest(
        NotificationDispatchId dispatchId,
        RequestContext requestContext
) {
    public RetryNotificationRequest {
        dispatchId = Objects.requireNonNull(dispatchId, "dispatchId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
