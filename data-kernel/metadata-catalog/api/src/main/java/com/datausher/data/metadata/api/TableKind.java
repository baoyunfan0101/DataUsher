package com.datausher.data.metadata.api;

import java.util.Locale;
import java.util.Objects;

public record TableKind(String value) implements Comparable<TableKind> {
    public static final TableKind TABLE = new TableKind("table");
    public static final TableKind VIEW = new TableKind("view");
    public static final TableKind OTHER = new TableKind("other");

    public TableKind {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-');
        if (!value.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,126}");
        }
    }

    public static TableKind fromExternalValue(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "TABLE", "BASE TABLE" -> TABLE;
            case "VIEW", "SYSTEM VIEW" -> VIEW;
            default -> new TableKind(value);
        };
    }

    @Override
    public int compareTo(TableKind other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
