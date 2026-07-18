package com.datausher.platform.notification.core;

import com.datausher.platform.notification.api.NotificationContent;

import java.util.Map;

public interface NotificationTemplateRenderer {
    NotificationContent render(NotificationContent template, Map<String, String> parameters);
}
