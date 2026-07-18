package com.datausher.integration.contract;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeJobHandle;
import com.datausher.integration.compute.api.ComputeJobLogEntry;
import com.datausher.integration.compute.api.ComputeJobLogPage;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobResultPage;
import com.datausher.integration.compute.api.ComputeJobState;
import com.datausher.integration.compute.api.ComputeJobStatus;
import com.datausher.integration.compute.api.ComputeResultColumn;
import com.datausher.integration.compute.api.SqlEngineAdapter;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.compute.api.SqlExplainPlan;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComputeEngineAdapterContractTest {
    @Test
    void verifiesManagedLifecycleAndSqlExplain() {
        var adapter = new FixtureSqlAdapter();
        var context = new AdapterRequestContext(
                "request-1", Instant.parse("2026-07-18T02:00:00Z"), Map.of());
        var request = new ComputeJobRequest(
                "local-binding", "sql", "select 1", Map.of(), Map.of());

        var handle = ComputeEngineAdapterContract.verifyManagedJob(
                adapter, context, request, Set.of("secret"));
        var plan = SqlEngineAdapterContract.verifyExplain(
                adapter,
                context,
                new SqlExecutionRequest(
                        request.bindingId(), request.payload(), Map.of(), 100, Map.of())
        );

        assertEquals("job-1", handle.externalJobId());
        assertEquals("values", plan.content());
    }

    private static final class FixtureSqlAdapter implements SqlEngineAdapter {
        private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
                "fixture-sql",
                AdapterType.COMPUTE_ENGINE,
                "1.0.0",
                Set.of(
                        AdapterCapability.of(ComputeCapabilities.JOB_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.JOB_CANCELLATION),
                        AdapterCapability.of(ComputeCapabilities.JOB_LOGS),
                        AdapterCapability.of(ComputeCapabilities.JOB_RESULTS),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXPLAIN)
                ),
                Map.of()
        );

        @Override
        public ComputeJobHandle submit(
                AdapterRequestContext context,
                ComputeJobRequest request
        ) {
            return new ComputeJobHandle(
                    DESCRIPTOR.adapterId(), request.bindingId(), "job-1");
        }

        @Override
        public ComputeJobStatus status(
                AdapterRequestContext context,
                ComputeJobHandle handle
        ) {
            return new ComputeJobStatus(
                    handle, ComputeJobState.SUCCEEDED, Instant.EPOCH, "", Map.of());
        }

        @Override
        public void cancel(AdapterRequestContext context, ComputeJobHandle handle) {
        }

        @Override
        public ComputeJobLogPage readLogs(
                AdapterRequestContext context,
                ComputeJobHandle handle,
                long afterSequence,
                int limit
        ) {
            return new ComputeJobLogPage(handle, List.of(
                    new ComputeJobLogEntry(0, Instant.EPOCH, "INFO", "done", Map.of())
            ), 1, true);
        }

        @Override
        public ComputeJobResultPage readResult(
                AdapterRequestContext context,
                ComputeJobHandle handle,
                long offset,
                int limit
        ) {
            return new ComputeJobResultPage(
                    handle,
                    List.of(new ComputeResultColumn("value", "integer", false, Map.of())),
                    List.of(List.of(new IntegrationValue.DecimalValue(1))),
                    offset,
                    0,
                    false,
                    "",
                    Map.of()
            );
        }

        @Override
        public SqlExplainPlan explain(
                AdapterRequestContext context,
                SqlExecutionRequest request
        ) {
            return new SqlExplainPlan("text", "values", Map.of());
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    DESCRIPTOR.adapterId(), AdapterHealthStatus.UP,
                    Instant.EPOCH, "ready", Map.of());
        }
    }
}
