package com.datausher.data.lineage.api;

import java.util.Map;
import java.util.Objects;

public record LineageEdgeInput(
        LineageNodeRef upstream,
        LineageNodeRef downstream,
        LineageEdgeType type,
        Map<String, String> attributes
) {
    public LineageEdgeInput {
        upstream = Objects.requireNonNull(upstream, "upstream must not be null");
        downstream = Objects.requireNonNull(downstream, "downstream must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        attributes = LineageValues.attributes(attributes);
        if (upstream.equals(downstream)) {
            throw new IllegalArgumentException("lineage edge must connect different nodes");
        }
    }
}
