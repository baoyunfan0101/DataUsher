package com.datausher.data.lineage.api;

import java.util.Objects;
import java.util.Set;

public record ImpactCandidate(
        LineageNode node,
        int distance,
        Set<LineageEdgeType> evidenceTypes
) {
    public ImpactCandidate {
        node = Objects.requireNonNull(node, "node must not be null");
        evidenceTypes = Set.copyOf(evidenceTypes);
        if (distance < 1) {
            throw new IllegalArgumentException("distance must be greater than zero");
        }
    }
}
