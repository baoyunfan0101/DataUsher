package com.datausher.data.lineage.api;

import java.util.Objects;
import java.util.Set;

public record ImpactAnalysisRequest(
        LineageNodeId changedNodeId,
        int maxDepth,
        int maxNodes,
        Set<LineageEdgeType> edgeTypes,
        Set<LineageNodeType> impactedNodeTypes
) {
    public ImpactAnalysisRequest {
        changedNodeId = Objects.requireNonNull(
                changedNodeId, "changedNodeId must not be null");
        edgeTypes = edgeTypes == null ? Set.of() : Set.copyOf(edgeTypes);
        impactedNodeTypes = impactedNodeTypes == null
                ? Set.of() : Set.copyOf(impactedNodeTypes);
        if (maxDepth < 1 || maxDepth > 100 || maxNodes < 1 || maxNodes > 10000) {
            throw new IllegalArgumentException(
                    "maxDepth must be 1..100 and maxNodes must be 1..10000");
        }
    }
}
