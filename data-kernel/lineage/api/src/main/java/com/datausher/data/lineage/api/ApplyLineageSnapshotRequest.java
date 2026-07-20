package com.datausher.data.lineage.api;

import com.datausher.platform.shared.context.RequestContext;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ApplyLineageSnapshotRequest(
        LineageSourceRef source,
        long sourceRevision,
        LineageSnapshotMode mode,
        List<LineageNodeInput> nodes,
        List<LineageEdgeInput> edges,
        Instant observedAt,
        RequestContext requestContext
) {
    public ApplyLineageSnapshotRequest {
        source = Objects.requireNonNull(source, "source must not be null");
        mode = Objects.requireNonNull(mode, "mode must not be null");
        nodes = List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (sourceRevision < 1) {
            throw new IllegalArgumentException("sourceRevision must be greater than zero");
        }
        Set<LineageNodeRef> references = new HashSet<>();
        for (LineageNodeInput node : nodes) {
            if (!references.add(node.reference())) {
                throw new IllegalArgumentException("lineage node references must be unique");
            }
        }
        Set<String> edgeKeys = new HashSet<>();
        for (LineageEdgeInput edge : edges) {
            if (!references.contains(edge.upstream()) || !references.contains(edge.downstream())) {
                throw new IllegalArgumentException(
                        "lineage edge endpoints must be declared in the same snapshot");
            }
            String key = edge.upstream() + "\u0000" + edge.downstream()
                    + "\u0000" + edge.type().value();
            if (!edgeKeys.add(key)) {
                throw new IllegalArgumentException("lineage edges must be unique");
            }
        }
    }
}
