package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageNodeInput;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageSourceRef;

import java.time.Instant;
import java.util.Objects;

public record LineageNodeContribution(
        LineageSourceRef source,
        long sourceRevision,
        LineageNodeId nodeId,
        LineageNodeInput node,
        Instant observedAt
) {
    public LineageNodeContribution {
        source = Objects.requireNonNull(source, "source must not be null");
        nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        node = Objects.requireNonNull(node, "node must not be null");
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (sourceRevision < 1) {
            throw new IllegalArgumentException("sourceRevision must be greater than zero");
        }
    }
}
