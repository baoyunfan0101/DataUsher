package com.datausher.data.metadata.api;

import java.util.Locale;
import java.util.Objects;

public record MetadataId(String value) implements Comparable<MetadataId> {
    public MetadataId {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9._-]{0,254}")) {
            throw new IllegalArgumentException(
                    "value must match [a-z][a-z0-9._-]{0,254}");
        }
    }

    @Override
    public int compareTo(MetadataId other) {
        return value.compareTo(Objects.requireNonNull(other, "other must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
