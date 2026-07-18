package com.datausher.development.api;

import java.util.Objects;

public record ScriptId(String value) implements Comparable<ScriptId> {
    public ScriptId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (!value.matches("[a-zA-Z0-9][a-zA-Z0-9._:-]{0,254}")) {
            throw new IllegalArgumentException("value contains unsupported characters");
        }
    }

    @Override
    public int compareTo(ScriptId other) {
        return value.compareTo(other.value);
    }
}
