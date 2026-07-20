package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.ApplyLineageSnapshotRequest;
import com.datausher.data.lineage.api.ImpactAnalysisRequest;
import com.datausher.data.lineage.api.LineageDirection;
import com.datausher.data.lineage.api.LineageEdgeInput;
import com.datausher.data.lineage.api.LineageEdgeQuery;
import com.datausher.data.lineage.api.LineageEdgeType;
import com.datausher.data.lineage.api.LineageNodeInput;
import com.datausher.data.lineage.api.LineageNodeQuery;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageNodeType;
import com.datausher.data.lineage.api.LineageSnapshotMode;
import com.datausher.data.lineage.api.LineageSourceRef;
import com.datausher.data.lineage.api.LineageSourceType;
import com.datausher.data.lineage.api.LineageTraversalQuery;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.page.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLineageServiceTest {
    @Test
    void appliesSourceSnapshotsIdempotentlyAndRejectsStaleRevisions() {
        var service = service();
        LineageSourceRef source = source("execution-1");
        ApplyLineageSnapshotRequest request = snapshot(
                source, 1, LineageSnapshotMode.REPLACE,
                List.of(node("raw"), node("curated")),
                List.of(edge("raw", "curated")));

        var first = service.applySnapshot(request);
        var duplicate = service.applySnapshot(request);

        assertTrue(first.changed());
        assertFalse(duplicate.changed());
        assertThrows(RevisionConflictException.class, () -> service.applySnapshot(
                snapshot(source, 1, LineageSnapshotMode.REPLACE,
                        List.of(node("raw")), List.of())));
    }

    @Test
    void replacesOnlyTheSelectedSourcesEvidence() {
        var service = service();
        service.applySnapshot(snapshot(
                source("execution-1"), 1, LineageSnapshotMode.REPLACE,
                List.of(node("raw"), node("curated")),
                List.of(edge("raw", "curated"))));
        service.applySnapshot(snapshot(
                source("workflow-1"), 1, LineageSnapshotMode.REPLACE,
                List.of(node("raw"), node("curated")),
                List.of(edge("raw", "curated"))));

        service.applySnapshot(snapshot(
                source("execution-1"), 2, LineageSnapshotMode.REPLACE,
                List.of(), List.of()));

        assertEquals(2, service.searchNodes(
                LineageNodeQuery.all(), PageRequest.firstPage()).total());
        assertEquals(1, service.searchEdges(
                LineageEdgeQuery.all(), PageRequest.firstPage()).total());
    }

    @Test
    void traversesCyclesSafelyAndReportsDownstreamImpact() {
        var service = service();
        service.applySnapshot(snapshot(
                source("execution-1"), 1, LineageSnapshotMode.REPLACE,
                List.of(node("raw"), node("curated"), node("report")),
                List.of(
                        edge("raw", "curated"),
                        edge("curated", "report"),
                        edge("report", "raw"))));
        var raw = service.findNode(ref("raw")).orElseThrow();

        var traversal = service.traverse(new LineageTraversalQuery(
                raw.nodeId(), LineageDirection.DOWNSTREAM, 10, 100,
                Set.of(), Set.of()));
        var impact = service.analyzeImpact(new ImpactAnalysisRequest(
                raw.nodeId(), 10, 100, Set.of(), Set.of()));

        assertEquals(3, traversal.nodes().size());
        assertFalse(traversal.truncated());
        assertEquals(List.of("curated", "report"), impact.candidates().stream()
                .map(candidate -> candidate.node().displayName()).toList());
        assertEquals(2L, impact.countsByType().get(LineageNodeType.TABLE));
    }

    @Test
    void impactAnalysisTraversesThroughNonImpactedNodeTypes() {
        var service = service();
        service.applySnapshot(snapshot(
                source("execution-1"), 1, LineageSnapshotMode.REPLACE,
                List.of(
                        node("raw", LineageNodeType.TABLE),
                        node("refresh-task", LineageNodeType.TASK),
                        node("dashboard", LineageNodeType.DASHBOARD)),
                List.of(
                        edge("raw", LineageNodeType.TABLE,
                                "refresh-task", LineageNodeType.TASK),
                        edge("refresh-task", LineageNodeType.TASK,
                                "dashboard", LineageNodeType.DASHBOARD))));
        var raw = service.findNode(ref("raw", LineageNodeType.TABLE)).orElseThrow();

        var impact = service.analyzeImpact(new ImpactAnalysisRequest(
                raw.nodeId(), 10, 100, Set.of(), Set.of(LineageNodeType.DASHBOARD)));

        assertEquals(List.of("dashboard"), impact.candidates().stream()
                .map(candidate -> candidate.node().displayName()).toList());
        assertEquals(1L, impact.countsByType().get(LineageNodeType.DASHBOARD));
    }

    private static DefaultLineageService service() {
        return new DefaultLineageService(
                new InMemoryLineageStore(), new Sha256LineageIdFactory(),
                new com.datausher.platform.shared.time.Clock() {
                    @Override
                    public Instant now() {
                        return Instant.EPOCH;
                    }

                    @Override
                    public java.time.ZoneId zone() {
                        return java.time.ZoneId.of("UTC");
                    }
                }, new com.datausher.platform.shared.id.core.UuidIdGenerator(), event -> { });
    }

    private static ApplyLineageSnapshotRequest snapshot(
            LineageSourceRef source,
            long revision,
            LineageSnapshotMode mode,
            List<LineageNodeInput> nodes,
            List<LineageEdgeInput> edges
    ) {
        return new ApplyLineageSnapshotRequest(
                source, revision, mode, nodes, edges, Instant.EPOCH,
                RequestContext.system("request-" + revision, Instant.EPOCH));
    }

    private static LineageSourceRef source(String id) {
        return new LineageSourceRef(
                id.startsWith("workflow")
                        ? LineageSourceType.WORKFLOW : LineageSourceType.EXECUTION,
                id);
    }

    private static LineageNodeInput node(String id) {
        return node(id, LineageNodeType.TABLE);
    }

    private static LineageNodeInput node(String id, LineageNodeType type) {
        return new LineageNodeInput(ref(id, type), id, Map.of("domain", "sales"));
    }

    private static LineageNodeRef ref(String id) {
        return ref(id, LineageNodeType.TABLE);
    }

    private static LineageNodeRef ref(String id, LineageNodeType type) {
        return new LineageNodeRef(type, id);
    }

    private static LineageEdgeInput edge(String upstream, String downstream) {
        return edge(upstream, LineageNodeType.TABLE, downstream, LineageNodeType.TABLE);
    }

    private static LineageEdgeInput edge(
            String upstream,
            LineageNodeType upstreamType,
            String downstream,
            LineageNodeType downstreamType
    ) {
        return new LineageEdgeInput(
                ref(upstream, upstreamType), ref(downstream, downstreamType),
                LineageEdgeType.DATA_FLOW, Map.of());
    }
}
