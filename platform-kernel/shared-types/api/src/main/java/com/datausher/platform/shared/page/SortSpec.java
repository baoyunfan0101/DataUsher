package com.datausher.platform.shared.page;

import java.util.Objects;

public record SortSpec(String field, SortDirection direction) {
    public SortSpec {
        field = Objects.requireNonNull(field, "field must not be null").trim();
        direction = Objects.requireNonNull(direction, "direction must not be null");
        if (field.isEmpty()) {
            throw new IllegalArgumentException("field must not be blank");
        }
    }
}
