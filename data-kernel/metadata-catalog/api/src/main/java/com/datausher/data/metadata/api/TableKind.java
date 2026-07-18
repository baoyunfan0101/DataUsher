package com.datausher.data.metadata.api;

import java.util.Locale;

public enum TableKind {
    TABLE,
    VIEW,
    OTHER;

    public static TableKind fromExternalValue(String value) {
        if (value == null) {
            return OTHER;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "TABLE", "BASE TABLE" -> TABLE;
            case "VIEW", "SYSTEM VIEW" -> VIEW;
            default -> OTHER;
        };
    }
}
