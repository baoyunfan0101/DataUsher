package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageDirection;
import com.datausher.data.lineage.api.LineageEdge;
import com.datausher.data.lineage.api.LineageEdgeQuery;
import com.datausher.data.lineage.api.LineageEdgeType;
import com.datausher.data.lineage.api.LineageNode;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageNodeQuery;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageSnapshotMode;
import com.datausher.data.lineage.api.LineageSourceRef;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.page.SortDirection;
import com.datausher.platform.shared.page.SortSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InMemoryLineageStore implements LineageStore {
    private Map<LineageNodeId, LineageNode> nodes = Map.of();
    private Map<LineageNodeRef, LineageNodeId> nodeIndex = Map.of();
    private Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> contributions = Map.of();
    private Map<com.datausher.data.lineage.api.LineageEdgeId, LineageEdge> edges = Map.of();
    private Map<LineageSourceRef, LineageMutation> sourceMutations = Map.of();

    @Override
    public synchronized boolean apply(LineageMutation mutation) {
        LineageMutation currentMutation = sourceMutations.get(mutation.source());
        if (currentMutation != null) {
            if (mutation.sourceRevision() == currentMutation.sourceRevision()
                    && mutation.equals(currentMutation)) {
                return false;
            }
            if (mutation.sourceRevision() <= currentMutation.sourceRevision()) {
                throw new RevisionConflictException(
                        "lineage-source", sourceKey(mutation.source()),
                        mutation.sourceRevision(), currentMutation.sourceRevision());
            }
        }

        Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> nextContributions =
                copyContributions(contributions);
        Map<com.datausher.data.lineage.api.LineageEdgeId, LineageEdge> nextEdges =
                new HashMap<>(edges);
        if (mutation.mode() == LineageSnapshotMode.REPLACE) {
            nextContributions.values().forEach(values -> values.remove(mutation.source()));
            nextContributions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            nextEdges.entrySet().removeIf(
                    entry -> entry.getValue().source().equals(mutation.source()));
        }
        mutation.nodes().forEach((reference, contribution) -> nextContributions
                .computeIfAbsent(reference, ignored -> new HashMap<>())
                .put(mutation.source(), contribution));
        nextEdges.putAll(mutation.edges());

        Map<LineageNodeId, LineageNode> nextNodes = materializeNodes(nextContributions);
        Map<LineageNodeRef, LineageNodeId> nextIndex = new HashMap<>();
        nextNodes.values().forEach(node -> nextIndex.put(node.reference(), node.nodeId()));
        boolean changed = !nextNodes.equals(nodes) || !nextEdges.equals(edges);

        Map<LineageSourceRef, LineageMutation> nextSourceMutations =
                new HashMap<>(sourceMutations);
        nextSourceMutations.put(mutation.source(), mutation);
        contributions = immutableNested(nextContributions);
        nodes = Map.copyOf(nextNodes);
        nodeIndex = Map.copyOf(nextIndex);
        edges = Map.copyOf(nextEdges);
        sourceMutations = Map.copyOf(nextSourceMutations);
        return changed;
    }

    @Override
    public synchronized Optional<LineageNode> findNode(LineageNodeId nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public synchronized Optional<LineageNode> findNode(LineageNodeRef reference) {
        return Optional.ofNullable(nodeIndex.get(reference)).map(nodes::get);
    }

    @Override
    public synchronized PageResult<LineageNode> searchNodes(
            LineageNodeQuery query,
            PageRequest pageRequest
    ) {
        String text = query.text().toLowerCase(Locale.ROOT);
        List<LineageNode> matches = nodes.values().stream()
                .filter(node -> query.types().isEmpty()
                        || query.types().contains(node.reference().type()))
                .filter(node -> text.isEmpty() || matches(node, text))
                .sorted(nodeComparator(pageRequest))
                .toList();
        return page(matches, pageRequest);
    }

    @Override
    public synchronized PageResult<LineageEdge> searchEdges(
            LineageEdgeQuery query,
            PageRequest pageRequest
    ) {
        List<LineageEdge> matches = edges.values().stream()
                .filter(edge -> query.upstreamNodeId()
                        .map(value -> value.equals(edge.upstreamNodeId())).orElse(true))
                .filter(edge -> query.downstreamNodeId()
                        .map(value -> value.equals(edge.downstreamNodeId())).orElse(true))
                .filter(edge -> query.types().isEmpty() || query.types().contains(edge.type()))
                .filter(edge -> query.source().map(value -> value.equals(edge.source())).orElse(true))
                .sorted(edgeComparator(pageRequest))
                .toList();
        return page(matches, pageRequest);
    }

    @Override
    public synchronized List<LineageEdge> adjacentEdges(
            LineageNodeId nodeId,
            LineageDirection direction,
            Set<LineageEdgeType> edgeTypes
    ) {
        return edges.values().stream()
                .filter(edge -> edgeTypes.isEmpty() || edgeTypes.contains(edge.type()))
                .filter(edge -> switch (direction) {
                    case UPSTREAM -> edge.downstreamNodeId().equals(nodeId);
                    case DOWNSTREAM -> edge.upstreamNodeId().equals(nodeId);
                    case BOTH -> edge.upstreamNodeId().equals(nodeId)
                            || edge.downstreamNodeId().equals(nodeId);
                })
                .sorted(Comparator.comparing(LineageEdge::edgeId))
                .toList();
    }

    private Map<LineageNodeId, LineageNode> materializeNodes(
            Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> values
    ) {
        Map<LineageNodeId, LineageNode> result = new HashMap<>();
        values.forEach((reference, bySource) -> {
            LineageNodeContribution selected = bySource.values().stream()
                    .max(Comparator.comparing(LineageNodeContribution::observedAt)
                            .thenComparingLong(LineageNodeContribution::sourceRevision)
                            .thenComparing(value -> sourceKey(value.source())))
                    .orElseThrow();
            LineageNodeId nodeId = nodeIndex.get(reference);
            if (nodeId == null) {
                nodeId = selected.nodeId();
            }
            LineageNode existing = nodes.get(nodeId);
            java.time.Instant firstSeenAt = existing == null
                    ? bySource.values().stream().map(LineageNodeContribution::observedAt)
                    .min(java.time.Instant::compareTo).orElseThrow()
                    : existing.firstSeenAt();
            boolean same = existing != null
                    && existing.displayName().equals(selected.node().displayName())
                    && existing.attributes().equals(selected.node().attributes());
            result.put(nodeId, same ? existing : new LineageNode(
                    nodeId, reference, selected.node().displayName(), selected.node().attributes(),
                    firstSeenAt, selected.observedAt(), existing == null ? 1 : existing.revision() + 1));
        });
        return result;
    }

    private static Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>>
    copyContributions(
            Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> source
    ) {
        Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> result = new HashMap<>();
        source.forEach((reference, values) -> result.put(reference, new HashMap<>(values)));
        return result;
    }

    private static Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>>
    immutableNested(
            Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> source
    ) {
        Map<LineageNodeRef, Map<LineageSourceRef, LineageNodeContribution>> result = new HashMap<>();
        source.forEach((reference, values) -> result.put(reference, Map.copyOf(values)));
        return Map.copyOf(result);
    }

    private static boolean matches(LineageNode node, String text) {
        return node.displayName().toLowerCase(Locale.ROOT).contains(text)
                || node.reference().externalId().toLowerCase(Locale.ROOT).contains(text)
                || node.attributes().entrySet().stream().anyMatch(entry ->
                entry.getKey().toLowerCase(Locale.ROOT).contains(text)
                        || entry.getValue().toLowerCase(Locale.ROOT).contains(text));
    }

    private static Comparator<LineageNode> nodeComparator(PageRequest request) {
        Comparator<LineageNode> comparator = null;
        for (SortSpec sort : request.sort()) {
            Comparator<LineageNode> next = switch (sort.field()) {
                case "nodeId" -> Comparator.comparing(LineageNode::nodeId);
                case "displayName" -> Comparator.comparing(LineageNode::displayName);
                case "type" -> Comparator.comparing(node -> node.reference().type().value());
                case "updatedAt" -> Comparator.comparing(LineageNode::updatedAt);
                default -> throw new IllegalArgumentException(
                        "unsupported lineage node sort field: " + sort.field());
            };
            if (sort.direction() == SortDirection.DESC) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? Comparator.comparing(LineageNode::nodeId)
                : comparator.thenComparing(LineageNode::nodeId);
    }

    private static Comparator<LineageEdge> edgeComparator(PageRequest request) {
        Comparator<LineageEdge> comparator = null;
        for (SortSpec sort : request.sort()) {
            Comparator<LineageEdge> next = switch (sort.field()) {
                case "edgeId" -> Comparator.comparing(LineageEdge::edgeId);
                case "type" -> Comparator.comparing(edge -> edge.type().value());
                case "observedAt" -> Comparator.comparing(LineageEdge::observedAt);
                case "upstreamNodeId" -> Comparator.comparing(LineageEdge::upstreamNodeId);
                case "downstreamNodeId" -> Comparator.comparing(LineageEdge::downstreamNodeId);
                default -> throw new IllegalArgumentException(
                        "unsupported lineage edge sort field: " + sort.field());
            };
            if (sort.direction() == SortDirection.DESC) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? Comparator.comparing(LineageEdge::edgeId)
                : comparator.thenComparing(LineageEdge::edgeId);
    }

    private static <T> PageResult<T> page(List<T> values, PageRequest request) {
        int fromIndex = (int) Math.min(request.offset(), values.size());
        int toIndex = Math.min(fromIndex + request.size(), values.size());
        return new PageResult<>(
                values.subList(fromIndex, toIndex), values.size(),
                request.page(), request.size());
    }

    private static String sourceKey(LineageSourceRef source) {
        return source.type().value() + ":" + source.sourceId();
    }
}
