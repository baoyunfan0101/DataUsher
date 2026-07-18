package com.datausher.data.datasource.api;

import java.util.Locale;
import java.util.Objects;

public enum DiscoveredObjectKind {
    DATABASE,
    TABLE,
    COLUMN,
    PARTITION,
    OTHER;

    public static DiscoveredObjectKind fromExternalKind(String value) {
        String normalized = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DATABASE", "CATALOG", "SCHEMA" -> DATABASE;
            case "TABLE", "VIEW" -> TABLE;
            case "COLUMN", "FIELD" -> COLUMN;
            case "PARTITION" -> PARTITION;
            default -> OTHER;
        };
    }
}
