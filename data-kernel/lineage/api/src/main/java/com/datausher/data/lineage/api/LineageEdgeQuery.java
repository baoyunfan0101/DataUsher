package com.datausher.data.lineage.api;

import java.util.Optional;
import java.util.Set;

public record LineageEdgeQuery(
        Optional<LineageNodeId> upstreamNodeId,
        Optional<LineageNodeId> downstreamNodeId,
        Set<LineageEdgeType> types,
        Optional<LineageSourceRef> source
) {
    public LineageEdgeQuery {
        upstreamNodeId = upstreamNodeId == null ? Optional.empty() : upstreamNodeId;
        downstreamNodeId = downstreamNodeId == null ? Optional.empty() : downstreamNodeId;
        types = types == null ? Set.of() : Set.copyOf(types);
        source = source == null ? Optional.empty() : source;
    }

    public static LineageEdgeQuery all() {
        return new LineageEdgeQuery(
                Optional.empty(), Optional.empty(), Set.of(), Optional.empty());
    }
}
