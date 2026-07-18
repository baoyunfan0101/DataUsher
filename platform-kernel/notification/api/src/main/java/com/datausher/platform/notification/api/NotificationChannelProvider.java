package com.datausher.platform.notification.api;

public interface NotificationChannelProvider {
    NotificationChannel channel();

    NotificationChannelResult deliver(NotificationEnvelope envelope);
}
