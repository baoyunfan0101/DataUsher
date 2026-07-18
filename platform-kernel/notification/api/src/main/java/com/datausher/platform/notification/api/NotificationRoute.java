package com.datausher.platform.notification.api;

import java.util.Objects;

public record NotificationRoute(
        NotificationChannel channel,
        NotificationContent content
) {
    public NotificationRoute {
        channel = Objects.requireNonNull(channel, "channel must not be null");
        content = Objects.requireNonNull(content, "content must not be null");
    }
}
