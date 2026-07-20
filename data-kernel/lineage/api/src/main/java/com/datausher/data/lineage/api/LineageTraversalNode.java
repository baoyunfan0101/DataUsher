package com.datausher.data.lineage.api;

import java.util.Objects;

public record LineageTraversalNode(LineageNode node, int depth) {
    public LineageTraversalNode {
        node = Objects.requireNonNull(node, "node must not be null");
        if (depth < 0) {
            throw new IllegalArgumentException("depth must not be negative");
        }
    }
}
