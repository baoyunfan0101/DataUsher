package com.datausher.development.api;

import java.util.Objects;

public record ScriptPublicationId(String value) {
    public ScriptPublicationId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
