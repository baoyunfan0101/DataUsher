package com.datausher.data.lineage.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ImpactAnalysisResult(
        LineageNode changedNode,
        List<ImpactCandidate> candidates,
        Map<LineageNodeType, Long> countsByType,
        boolean truncated
) {
    public ImpactAnalysisResult {
        changedNode = Objects.requireNonNull(changedNode, "changedNode must not be null");
        candidates = List.copyOf(candidates);
        countsByType = Map.copyOf(countsByType);
    }
}
