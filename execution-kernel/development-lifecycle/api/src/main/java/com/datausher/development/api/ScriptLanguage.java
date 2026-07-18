package com.datausher.development.api;

import java.util.Objects;

public record ScriptLanguage(String value) {
    public ScriptLanguage {
        value = Objects.requireNonNull(value, "value must not be null").trim().toLowerCase();
        if (!value.matches("[a-z][a-z0-9.+-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }
}
