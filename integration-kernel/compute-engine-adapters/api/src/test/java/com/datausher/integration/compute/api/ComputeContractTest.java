package com.datausher.integration.compute.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputeContractTest {
    @Test
    void exposesTerminalJobStates() {
        ComputeJobHandle handle = new ComputeJobHandle("spark", "warehouse", "job-1");

        assertTrue(new ComputeJobStatus(
                handle, ComputeJobState.SUCCEEDED, Instant.EPOCH, "", Map.of()).terminal());
        assertFalse(new ComputeJobStatus(
                handle, ComputeJobState.RUNNING, Instant.EPOCH, "", Map.of()).terminal());
    }

    @Test
    void rejectsInvalidRowLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new SqlExecutionRequest("warehouse", "select 1", Map.of(), 0, Map.of()));
        ComputeJobHandle handle = new ComputeJobHandle(" SPARK ", " WAREHOUSE ", "job-1");
        assertEquals("spark", handle.adapterId());
        assertEquals("warehouse", handle.bindingId());
    }

    @Test
    void acceptsDuplicateColumnNamesAndValidatesRowWidth() {
        ComputeJobHandle handle = new ComputeJobHandle("local", "warehouse", "job-1");
        List<ComputeResultColumn> columns = List.of(
                new ComputeResultColumn("id", "bigint", false, Map.of()),
                new ComputeResultColumn("id", "bigint", false, Map.of())
        );

        ComputeJobResultPage page = new ComputeJobResultPage(
                handle,
                columns,
                List.of(List.of(
                        new com.datausher.integration.runtime.api.IntegrationValue.DecimalValue(1),
                        new com.datausher.integration.runtime.api.IntegrationValue.DecimalValue(2)
                )),
                0,
                0,
                false,
                "",
                Map.of()
        );

        assertEquals(2, page.columns().size());
        assertThrows(IllegalArgumentException.class, () -> new ComputeJobResultPage(
                handle, columns, List.of(List.of()), 0, 0, false, "", Map.of()));
    }

    @Test
    void publishesCanonicalCapabilitiesForPortableDispatch() {
        assertTrue(ComputeCapabilities.JOB_EXECUTION.startsWith("compute."));
        assertTrue(ComputeCapabilities.JOB_LOGS.startsWith("compute."));
        assertTrue(ComputeCapabilities.JOB_RESULTS.startsWith("compute."));
        assertTrue(ComputeCapabilities.SQL_EXECUTION.startsWith("compute."));
        assertTrue(ComputeCapabilities.SQL_EXPLAIN.startsWith("compute."));
        assertTrue(ComputeCapabilities.SCRIPT_EXECUTION.startsWith("compute."));
    }
}
