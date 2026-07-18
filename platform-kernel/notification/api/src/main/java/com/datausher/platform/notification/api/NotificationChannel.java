package com.datausher.platform.notification.api;

import java.util.Objects;

public record NotificationChannel(String value) {
    public static final NotificationChannel EMAIL = new NotificationChannel("email");
    public static final NotificationChannel IN_APP = new NotificationChannel("in-app");
    public static final NotificationChannel WEBHOOK = new NotificationChannel("webhook");

    public NotificationChannel {
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
