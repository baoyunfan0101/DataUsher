package com.datausher.data.metadata.api;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record TableSchema(
        MetadataId tableId,
        long version,
        String fingerprint,
        List<ColumnSchema> columns,
        Instant recordedAt
) {
    public TableSchema {
        tableId = Objects.requireNonNull(tableId, "tableId must not be null");
        fingerprint = MetadataValues.requireText(fingerprint, "fingerprint");
        columns = columns == null ? List.of() : columns.stream()
                .sorted(Comparator.comparingInt(ColumnSchema::ordinalPosition))
                .toList();
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        HashSet<String> names = new HashSet<>();
        HashSet<Integer> positions = new HashSet<>();
        for (ColumnSchema column : columns) {
            if (!names.add(column.name())) {
                throw new IllegalArgumentException("duplicate column name: " + column.name());
            }
            if (!positions.add(column.ordinalPosition())) {
                throw new IllegalArgumentException(
                        "duplicate column position: " + column.ordinalPosition());
            }
        }
    }
}
