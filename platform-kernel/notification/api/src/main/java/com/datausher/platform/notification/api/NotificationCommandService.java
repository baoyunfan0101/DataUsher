package com.datausher.platform.notification.api;

public interface NotificationCommandService {
    NotificationTemplate publishTemplate(PublishNotificationTemplateRequest request);

    NotificationDispatch send(SendNotificationRequest request);

    NotificationDispatch retry(RetryNotificationRequest request);
}
