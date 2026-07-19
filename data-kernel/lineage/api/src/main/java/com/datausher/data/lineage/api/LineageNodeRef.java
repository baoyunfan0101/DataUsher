package com.datausher.data.lineage.api;

import java.util.Objects;

public record LineageNodeRef(LineageNodeType type, String externalId) {
    public LineageNodeRef {
        type = Objects.requireNonNull(type, "type must not be null");
        externalId = LineageValues.text(externalId, "externalId");
    }
}
