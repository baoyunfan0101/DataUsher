package com.datausher.development.api;

import java.util.Objects;

public record DebugRunId(String value) {
    public DebugRunId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
