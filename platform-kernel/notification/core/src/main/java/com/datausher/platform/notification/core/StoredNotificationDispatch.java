package com.datausher.platform.notification.core;

import com.datausher.platform.notification.api.NotificationContent;
import com.datausher.platform.notification.api.NotificationDispatch;

import java.util.Map;
import java.util.Objects;

public record StoredNotificationDispatch(
        NotificationDispatch dispatch,
        String requestFingerprint,
        Map<String, NotificationContent> renderedContents
) {
    public StoredNotificationDispatch {
        dispatch = Objects.requireNonNull(dispatch, "dispatch must not be null");
        requestFingerprint = Objects.requireNonNull(
                requestFingerprint, "requestFingerprint must not be null").trim();
        renderedContents = Map.copyOf(Objects.requireNonNull(
                renderedContents, "renderedContents must not be null"));
        if (requestFingerprint.isEmpty() || renderedContents.size() != dispatch.deliveries().size()) {
            throw new IllegalArgumentException("fingerprint and content for every delivery are required");
        }
    }
}
