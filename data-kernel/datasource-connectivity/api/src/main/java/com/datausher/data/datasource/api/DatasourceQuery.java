package com.datausher.data.datasource.api;

import java.util.Locale;

public record DatasourceQuery(String text, String adapterId, DatasourceStatus status) {
    public DatasourceQuery {
        text = normalizeNullable(text, false);
        adapterId = normalizeNullable(adapterId, true);
    }

    public static DatasourceQuery all() {
        return new DatasourceQuery(null, null, null);
    }

    private static String normalizeNullable(String value, boolean lowerCase) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return lowerCase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }
}
