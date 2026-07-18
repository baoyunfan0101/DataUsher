package com.datausher.data.datasource.api;

import java.util.Locale;
import java.util.Objects;

public record DiscoveredObjectKind(String value) implements Comparable<DiscoveredObjectKind> {
    public static final DiscoveredObjectKind DATABASE = new DiscoveredObjectKind("database");
    public static final DiscoveredObjectKind TABLE = new DiscoveredObjectKind("table");
    public static final DiscoveredObjectKind COLUMN = new DiscoveredObjectKind("column");
    public static final DiscoveredObjectKind PARTITION = new DiscoveredObjectKind("partition");

    public DiscoveredObjectKind {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,126}");
        }
    }

    public static DiscoveredObjectKind fromExternalKind(String value) {
        String normalized = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "database", "catalog", "schema" -> DATABASE;
            case "table", "view" -> TABLE;
            case "column", "field" -> COLUMN;
            case "partition" -> PARTITION;
            default -> new DiscoveredObjectKind(normalized);
        };
    }

    @Override
    public int compareTo(DiscoveredObjectKind other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
