package com.datausher.platform.notification.core;

import com.datausher.platform.notification.api.NotificationContent;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrictNotificationTemplateRenderer implements NotificationTemplateRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z][a-zA-Z0-9._-]{0,126})}");

    @Override
    public NotificationContent render(NotificationContent template, Map<String, String> parameters) {
        Objects.requireNonNull(template, "template must not be null");
        Map<String, String> safeParameters = Map.copyOf(Objects.requireNonNull(
                parameters, "parameters must not be null"));
        return new NotificationContent(
                template.contentType(),
                renderValue(template.subject(), safeParameters),
                renderValue(template.body(), safeParameters),
                template.attributes()
        );
    }

    private static String renderValue(String value, Map<String, String> parameters) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = parameters.get(name);
            if (replacement == null) {
                throw new IllegalArgumentException("notification parameter is required: " + name);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
