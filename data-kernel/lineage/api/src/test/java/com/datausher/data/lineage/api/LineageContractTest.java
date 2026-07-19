package com.datausher.data.lineage.api;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineageContractTest {
    @Test
    void preservesOpenNodeEdgeAndSourceTypes() {
        LineageNodeType nodeType = new LineageNodeType("feature-view");
        LineageEdgeType edgeType = new LineageEdgeType("materializes");
        LineageSourceType sourceType = new LineageSourceType("stream-processor");

        assertEquals("feature-view", nodeType.value());
        assertEquals("materializes", edgeType.value());
        assertEquals("stream-processor", sourceType.value());
    }

    @Test
    void requiresEveryEdgeEndpointInTheAtomicSnapshot() {
        LineageNodeRef upstream = new LineageNodeRef(LineageNodeType.TABLE, "catalog.orders");
        LineageNodeRef downstream = new LineageNodeRef(LineageNodeType.TABLE, "catalog.summary");

        assertThrows(IllegalArgumentException.class, () -> new ApplyLineageSnapshotRequest(
                new LineageSourceRef(LineageSourceType.EXECUTION, "execution-1"), 1,
                LineageSnapshotMode.REPLACE,
                List.of(new LineageNodeInput(upstream, "Orders", Map.of())),
                List.of(new LineageEdgeInput(
                        upstream, downstream, LineageEdgeType.DATA_FLOW, Map.of())),
                Instant.EPOCH, RequestContext.system("request-1", Instant.EPOCH)));
    }

    @Test
    void boundsGraphTraversalWork() {
        assertThrows(IllegalArgumentException.class, () -> new LineageTraversalQuery(
                new LineageNodeId("node-1"), LineageDirection.DOWNSTREAM,
                101, 100, java.util.Set.of(), java.util.Set.of()));
    }
}
