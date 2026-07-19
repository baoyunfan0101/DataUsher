package com.datausher.data.lineage.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record LineageEdge(
        LineageEdgeId edgeId,
        LineageNodeId upstreamNodeId,
        LineageNodeId downstreamNodeId,
        LineageEdgeType type,
        LineageSourceRef source,
        Map<String, String> attributes,
        Instant observedAt,
        long sourceRevision
) {
    public LineageEdge {
        edgeId = Objects.requireNonNull(edgeId, "edgeId must not be null");
        upstreamNodeId = Objects.requireNonNull(
                upstreamNodeId, "upstreamNodeId must not be null");
        downstreamNodeId = Objects.requireNonNull(
                downstreamNodeId, "downstreamNodeId must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        source = Objects.requireNonNull(source, "source must not be null");
        attributes = LineageValues.attributes(attributes);
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (upstreamNodeId.equals(downstreamNodeId) || sourceRevision < 1) {
            throw new IllegalArgumentException("lineage edge has invalid identity or revision");
        }
    }
}
