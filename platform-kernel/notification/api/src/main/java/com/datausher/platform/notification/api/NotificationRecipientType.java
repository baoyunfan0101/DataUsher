package com.datausher.platform.notification.api;

import java.util.Objects;

public record NotificationRecipientType(String value) {
    public static final NotificationRecipientType SUBJECT = new NotificationRecipientType("subject");
    public static final NotificationRecipientType ADDRESS = new NotificationRecipientType("address");
    public static final NotificationRecipientType TOPIC = new NotificationRecipientType("topic");

    public NotificationRecipientType {
        value = Objects.requireNonNull(value, "value must not be null").trim().toLowerCase();
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value must match [a-z][a-z0-9.-]{0,126}");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
