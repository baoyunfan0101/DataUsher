package com.datausher.platform.shared.id;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record IdFormat(String value) {
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("[a-z0-9][a-z0-9_-]*(\\.[a-z0-9][a-z0-9_-]*)*");

    public static final IdFormat UUID = IdFormat.of("uuid");
    public static final IdFormat ULID = IdFormat.of("ulid");
    public static final IdFormat SNOWFLAKE = IdFormat.of("snowflake");
    public static final IdFormat SEQUENCE = IdFormat.of("sequence");
    public static final IdFormat CUSTOM = IdFormat.of("custom");

    public IdFormat {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be a canonical ID format");
        }
    }

    public static IdFormat of(String value) {
        return new IdFormat(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
