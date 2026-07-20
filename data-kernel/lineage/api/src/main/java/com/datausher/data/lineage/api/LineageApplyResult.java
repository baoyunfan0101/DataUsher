package com.datausher.data.lineage.api;

import java.time.Instant;
import java.util.Objects;

public record LineageApplyResult(
        LineageSourceRef source,
        long sourceRevision,
        int nodeCount,
        int edgeCount,
        boolean changed,
        Instant appliedAt
) {
    public LineageApplyResult {
        source = Objects.requireNonNull(source, "source must not be null");
        appliedAt = Objects.requireNonNull(appliedAt, "appliedAt must not be null");
        if (sourceRevision < 1 || nodeCount < 0 || edgeCount < 0) {
            throw new IllegalArgumentException("lineage result contains invalid counts");
        }
    }
}
