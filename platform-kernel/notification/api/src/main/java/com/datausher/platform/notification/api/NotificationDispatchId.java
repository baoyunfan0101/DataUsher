package com.datausher.platform.notification.api;

import java.util.Objects;

public record NotificationDispatchId(String value) {
    public NotificationDispatchId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
