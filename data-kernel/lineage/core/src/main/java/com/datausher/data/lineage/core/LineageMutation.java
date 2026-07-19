package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageEdge;
import com.datausher.data.lineage.api.LineageEdgeId;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageSnapshotMode;
import com.datausher.data.lineage.api.LineageSourceRef;

import java.util.Map;
import java.util.Objects;

public record LineageMutation(
        LineageSourceRef source,
        long sourceRevision,
        LineageSnapshotMode mode,
        Map<LineageNodeRef, LineageNodeContribution> nodes,
        Map<LineageEdgeId, LineageEdge> edges
) {
    public LineageMutation {
        source = Objects.requireNonNull(source, "source must not be null");
        mode = Objects.requireNonNull(mode, "mode must not be null");
        nodes = Map.copyOf(nodes);
        edges = Map.copyOf(edges);
        if (sourceRevision < 1) {
            throw new IllegalArgumentException("sourceRevision must be greater than zero");
        }
    }
}
