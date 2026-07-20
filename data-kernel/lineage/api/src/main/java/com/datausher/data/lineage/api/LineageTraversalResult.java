package com.datausher.data.lineage.api;

import java.util.List;
import java.util.Objects;

public record LineageTraversalResult(
        LineageNode root,
        List<LineageTraversalNode> nodes,
        List<LineageEdge> edges,
        int reachedDepth,
        boolean truncated
) {
    public LineageTraversalResult {
        root = Objects.requireNonNull(root, "root must not be null");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        if (reachedDepth < 0) {
            throw new IllegalArgumentException("reachedDepth must not be negative");
        }
    }
}
