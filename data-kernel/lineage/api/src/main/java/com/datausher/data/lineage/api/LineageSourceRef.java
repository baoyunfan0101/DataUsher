package com.datausher.data.lineage.api;

import java.util.Objects;

public record LineageSourceRef(LineageSourceType type, String sourceId) {
    public LineageSourceRef {
        type = Objects.requireNonNull(type, "type must not be null");
        sourceId = LineageValues.text(sourceId, "sourceId");
    }
}
