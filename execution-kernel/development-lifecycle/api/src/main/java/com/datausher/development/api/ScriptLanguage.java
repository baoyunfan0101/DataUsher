package com.datausher.development.api;

import java.util.Locale;
import java.util.Objects;

public record ScriptLanguage(String value) {
    public ScriptLanguage {
        value = Objects.requireNonNull(value, "value must not be null").trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.+-]{0,126}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }
}
