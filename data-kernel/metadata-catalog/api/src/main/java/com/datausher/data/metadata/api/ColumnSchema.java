package com.datausher.data.metadata.api;

import java.util.Objects;

public record ColumnSchema(
        String name,
        int ordinalPosition,
        String dataType,
        boolean nullable
) {
    public ColumnSchema {
        name = MetadataValues.requireText(name, "name");
        dataType = MetadataValues.requireText(dataType, "dataType");
        if (ordinalPosition < 1) {
            throw new IllegalArgumentException("ordinalPosition must be greater than zero");
        }
    }
}
