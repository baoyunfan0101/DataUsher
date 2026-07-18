package com.datausher.data.metadata.api;

import java.util.Locale;
import java.util.Objects;

public record MetadataAssetType(String value) implements Comparable<MetadataAssetType> {
    public static final MetadataAssetType CATALOG = new MetadataAssetType("catalog");
    public static final MetadataAssetType DATABASE = new MetadataAssetType("database");
    public static final MetadataAssetType TABLE = new MetadataAssetType("table");
    public static final MetadataAssetType COLUMN = new MetadataAssetType("column");

    public MetadataAssetType {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9._-]{0,126}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,126}");
        }
    }

    @Override
    public int compareTo(MetadataAssetType other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
