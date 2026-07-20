package com.datausher.integration.runtime.api;

import java.util.Objects;

public final class SecretString {
    private final String value;

    public SecretString(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
    }

    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "[secret]";
    }
}
