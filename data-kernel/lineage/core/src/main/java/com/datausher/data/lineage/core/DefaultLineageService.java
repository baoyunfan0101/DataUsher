package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.ApplyLineageSnapshotRequest;
import com.datausher.data.lineage.api.ImpactAnalysisRequest;
import com.datausher.data.lineage.api.ImpactAnalysisResult;
import com.datausher.data.lineage.api.ImpactAnalysisService;
import com.datausher.data.lineage.api.ImpactCandidate;
import com.datausher.data.lineage.api.LineageApplyResult;
import com.datausher.data.lineage.api.LineageCommandService;
import com.datausher.data.lineage.api.LineageDirection;
import com.datausher.data.lineage.api.LineageEdge;
import com.datausher.data.lineage.api.LineageEdgeId;
import com.datausher.data.lineage.api.LineageEdgeInput;
import com.datausher.data.lineage.api.LineageEdgeQuery;
import com.datausher.data.lineage.api.LineageEdgeType;
import com.datausher.data.lineage.api.LineageNode;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageNodeInput;
import com.datausher.data.lineage.api.LineageNodeQuery;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageNodeType;
import com.datausher.data.lineage.api.LineageQueryService;
import com.datausher.data.lineage.api.LineageTraversalNode;
import com.datausher.data.lineage.api.LineageTraversalQuery;
import com.datausher.data.lineage.api.LineageTraversalResult;
import com.datausher.data.lineage.api.LineageUpdatedEvent;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DefaultLineageService
        implements LineageCommandService, LineageQueryService, ImpactAnalysisService {
    private final LineageStore store;
    private final LineageIdFactory idFactory;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;

    public DefaultLineageService(
            LineageStore store,
            LineageIdFactory idFactory,
            Clock clock,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idFactory = Objects.requireNonNull(idFactory, "idFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public LineageApplyResult applySnapshot(ApplyLineageSnapshotRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Map<LineageNodeRef, LineageNodeId> nodeIds = request.nodes().stream()
                .collect(Collectors.toUnmodifiableMap(
                        LineageNodeInput::reference,
                        node -> idFactory.nodeId(node.reference())));
        Map<LineageNodeRef, LineageNodeContribution> nodes = request.nodes().stream()
                .collect(Collectors.toUnmodifiableMap(
                        LineageNodeInput::reference,
                        node -> new LineageNodeContribution(
                                request.source(), request.sourceRevision(),
                                nodeIds.get(node.reference()), node, request.observedAt())));
        Map<LineageEdgeId, LineageEdge> edges = request.edges().stream()
                .collect(Collectors.toUnmodifiableMap(
                        edge -> idFactory.edgeId(request.source(), edge),
                        edge -> toEdge(request, edge, nodeIds)));
        boolean changed = store.apply(new LineageMutation(
                request.source(), request.sourceRevision(), request.mode(), nodes, edges));
        LineageApplyResult result = new LineageApplyResult(
                request.source(), request.sourceRevision(), nodes.size(), edges.size(),
                changed, clock.now());
        if (changed) {
            eventPublisher.publish(new LineageUpdatedEvent(
                    nextEventId(), result.appliedAt(), request.requestContext(), result));
        }
        return result;
    }

    @Override
    public Optional<LineageNode> findNode(LineageNodeId nodeId) {
        return store.findNode(Objects.requireNonNull(nodeId, "nodeId must not be null"));
    }

    @Override
    public Optional<LineageNode> findNode(LineageNodeRef reference) {
        return store.findNode(Objects.requireNonNull(reference, "reference must not be null"));
    }

    @Override
    public PageResult<LineageNode> searchNodes(
            LineageNodeQuery query,
            PageRequest pageRequest
    ) {
        return store.searchNodes(
                Objects.requireNonNull(query, "query must not be null"),
                Objects.requireNonNull(pageRequest, "pageRequest must not be null"));
    }

    @Override
    public PageResult<LineageEdge> searchEdges(
            LineageEdgeQuery query,
            PageRequest pageRequest
    ) {
        return store.searchEdges(
                Objects.requireNonNull(query, "query must not be null"),
                Objects.requireNonNull(pageRequest, "pageRequest must not be null"));
    }

    @Override
    public LineageTraversalResult traverse(LineageTraversalQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        LineageNode root = store.findNode(query.rootNodeId())
                .orElseThrow(() -> new IllegalArgumentException("lineage root node does not exist"));
        Map<LineageNodeId, LineageTraversalNode> visited = new LinkedHashMap<>();
        Map<LineageEdgeId, LineageEdge> resultEdges = new LinkedHashMap<>();
        ArrayDeque<NodeDepth> pending = new ArrayDeque<>();
        visited.put(root.nodeId(), new LineageTraversalNode(root, 0));
        pending.add(new NodeDepth(root.nodeId(), 0));
        int reachedDepth = 0;
        boolean truncated = false;

        while (!pending.isEmpty() && !truncated) {
            NodeDepth current = pending.removeFirst();
            if (current.depth() >= query.maxDepth()) {
                continue;
            }
            for (LineageEdge edge : store.adjacentEdges(
                    current.nodeId(), query.direction(), query.edgeTypes())) {
                LineageNodeId adjacentId = adjacentNode(edge, current.nodeId(), query.direction());
                LineageNode adjacent = store.findNode(adjacentId).orElseThrow(() ->
                        new IllegalStateException("lineage edge references a missing node"));
                if (!query.nodeTypes().isEmpty()
                        && !query.nodeTypes().contains(adjacent.reference().type())) {
                    continue;
                }
                resultEdges.put(edge.edgeId(), edge);
                if (visited.containsKey(adjacentId)) {
                    continue;
                }
                if (visited.size() >= query.maxNodes()) {
                    truncated = true;
                    break;
                }
                int depth = current.depth() + 1;
                visited.put(adjacentId, new LineageTraversalNode(adjacent, depth));
                reachedDepth = Math.max(reachedDepth, depth);
                pending.addLast(new NodeDepth(adjacentId, depth));
            }
        }
        return new LineageTraversalResult(
                root, List.copyOf(visited.values()), List.copyOf(resultEdges.values()),
                reachedDepth, truncated);
    }

    @Override
    public ImpactAnalysisResult analyzeImpact(ImpactAnalysisRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        LineageTraversalResult traversal = traverse(new LineageTraversalQuery(
                request.changedNodeId(), LineageDirection.DOWNSTREAM,
                request.maxDepth(), request.maxNodes(), request.edgeTypes(),
                request.impactedNodeTypes()));
        Map<LineageNodeId, Set<LineageEdgeType>> evidence = new HashMap<>();
        traversal.edges().forEach(edge -> evidence
                .computeIfAbsent(edge.downstreamNodeId(), ignored -> new HashSet<>())
                .add(edge.type()));
        List<ImpactCandidate> candidates = traversal.nodes().stream()
                .filter(node -> node.depth() > 0)
                .map(node -> new ImpactCandidate(
                        node.node(), node.depth(),
                        evidence.getOrDefault(node.node().nodeId(), Set.of())))
                .sorted(Comparator.comparingInt(ImpactCandidate::distance)
                        .thenComparing(candidate -> candidate.node().nodeId()))
                .toList();
        Map<LineageNodeType, Long> counts = candidates.stream().collect(Collectors.groupingBy(
                candidate -> candidate.node().reference().type(), Collectors.counting()));
        return new ImpactAnalysisResult(
                traversal.root(), candidates, counts, traversal.truncated());
    }

    private LineageEdge toEdge(
            ApplyLineageSnapshotRequest request,
            LineageEdgeInput input,
            Map<LineageNodeRef, LineageNodeId> nodeIds
    ) {
        return new LineageEdge(
                idFactory.edgeId(request.source(), input),
                nodeIds.get(input.upstream()), nodeIds.get(input.downstream()),
                input.type(), request.source(), input.attributes(),
                request.observedAt(), request.sourceRevision());
    }

    private static LineageNodeId adjacentNode(
            LineageEdge edge,
            LineageNodeId current,
            LineageDirection direction
    ) {
        return switch (direction) {
            case UPSTREAM -> edge.upstreamNodeId();
            case DOWNSTREAM -> edge.downstreamNodeId();
            case BOTH -> edge.upstreamNodeId().equals(current)
                    ? edge.downstreamNodeId() : edge.upstreamNodeId();
        };
    }

    private record NodeDepth(LineageNodeId nodeId, int depth) {
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(
                IdGenerationRequest.of("lineage", "domain-event"));
    }
}
