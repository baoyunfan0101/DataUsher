package com.datausher.integration.compute.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
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
                () -> new SqlExecutionRequest("warehouse", "select 1", Map.of(), 0));
        ComputeJobHandle handle = new ComputeJobHandle(" SPARK ", " WAREHOUSE ", "job-1");
        assertEquals("spark", handle.adapterId());
        assertEquals("warehouse", handle.bindingId());
    }

    @Test
    void publishesCanonicalCapabilitiesForPortableDispatch() {
        assertTrue(ComputeCapabilities.JOB_EXECUTION.startsWith("compute."));
        assertTrue(ComputeCapabilities.SQL_EXECUTION.startsWith("compute."));
        assertTrue(ComputeCapabilities.SCRIPT_EXECUTION.startsWith("compute."));
    }
}
