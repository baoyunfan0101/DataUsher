package com.datausher.data.lineage.api;

import java.util.Objects;
import java.util.Set;

public record LineageTraversalQuery(
        LineageNodeId rootNodeId,
        LineageDirection direction,
        int maxDepth,
        int maxNodes,
        Set<LineageEdgeType> edgeTypes,
        Set<LineageNodeType> nodeTypes
) {
    public LineageTraversalQuery {
        rootNodeId = Objects.requireNonNull(rootNodeId, "rootNodeId must not be null");
        direction = Objects.requireNonNull(direction, "direction must not be null");
        edgeTypes = edgeTypes == null ? Set.of() : Set.copyOf(edgeTypes);
        nodeTypes = nodeTypes == null ? Set.of() : Set.copyOf(nodeTypes);
        if (maxDepth < 1 || maxDepth > 100 || maxNodes < 1 || maxNodes > 10000) {
            throw new IllegalArgumentException(
                    "maxDepth must be 1..100 and maxNodes must be 1..10000");
        }
    }
}
