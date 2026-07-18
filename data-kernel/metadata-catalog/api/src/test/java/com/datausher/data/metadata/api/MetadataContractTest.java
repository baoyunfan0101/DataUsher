package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataContractTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void rejectsAmbiguousColumnPositionsInASchema() {
        assertThrows(IllegalArgumentException.class, () -> new TableSchema(
                new MetadataId("table-1"),
                1,
                "fingerprint",
                List.of(
                        new ColumnSchema("id", 1, "bigint", false),
                        new ColumnSchema("name", 1, "varchar", true)
                ),
                NOW
        ));
    }

    @Test
    void requiresAFullSnapshotForReplaceSynchronization() {
        DatasourceDiscoverySnapshot partial = new DatasourceDiscoverySnapshot(
                "discovery-1",
                new DatasourceId("analytics"),
                "analytics",
                List.of(),
                NOW
        );

        assertThrows(IllegalArgumentException.class, () -> new SynchronizeMetadataRequest(
                partial,
                "Analytics",
                MetadataSyncMode.REPLACE,
                RequestContext.system("request-1", NOW)
        ));
    }
}
