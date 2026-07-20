package com.datausher.data.lineage.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record LineageNode(
        LineageNodeId nodeId,
        LineageNodeRef reference,
        String displayName,
        Map<String, String> attributes,
        Instant firstSeenAt,
        Instant updatedAt,
        long revision
) {
    public LineageNode {
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        reference = Objects.requireNonNull(reference, "reference must not be null");
        displayName = LineageValues.text(displayName, "displayName");
        attributes = LineageValues.attributes(attributes);
        firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(firstSeenAt) || revision < 1) {
            throw new IllegalArgumentException("lineage node has invalid audit fields");
        }
    }
}
