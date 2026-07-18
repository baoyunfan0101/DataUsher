package com.datausher.execution.core;

import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.CreateExecutionQueueRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.ExplainExecutionRequest;
import com.datausher.execution.api.ReadExecutionLogRequest;
import com.datausher.execution.api.ReadExecutionResultRequest;
import com.datausher.execution.api.RegisterExecutionAccountRequest;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeEngineAdapter;
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
import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.core.DefaultIntegrationErrorMapper;
import com.datausher.integration.runtime.core.InMemoryAdapterRegistry;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExecutionServiceTest {
    @Test
    void submitsDispatchesAndRefreshesWithoutBlockingSubmission() {
        Fixture fixture = new Fixture(1);

        var request = fixture.service.submit(fixture.submission("select 1"));

        assertEquals(ExecutionState.QUEUED, request.state());
        assertEquals(0, fixture.adapter.submissions);

        var instance = fixture.service.dispatchNext(fixture.queueId, fixture.context).orElseThrow();
        assertEquals(ExecutionState.RUNNING, instance.state());
        assertEquals(1, fixture.adapter.submissions);

        fixture.adapter.state = ComputeJobState.SUCCEEDED;
        var completed = fixture.service.refresh(instance.instanceId(), fixture.context);
        var page = fixture.service.search(ExecutionQuery.all(), PageRequest.firstPage());

        assertEquals(ExecutionState.SUCCEEDED, completed.state());
        assertEquals(ExecutionState.SUCCEEDED, page.items().getFirst().state());
        assertTrue(fixture.events.stream()
                .map(DomainEvent::eventType)
                .anyMatch("ExecutionCompleted"::equals));
    }

    @Test
    void enforcesQueueConcurrencyAndCancelsThroughAdapter() {
        Fixture fixture = new Fixture(1);
        var first = fixture.service.submit(fixture.submission("select 1"));
        fixture.service.submit(fixture.submission("select 2"));
        var instance = fixture.service.dispatchNext(
                fixture.queueId, fixture.context).orElseThrow();

        assertTrue(fixture.service.dispatchNext(fixture.queueId, fixture.context).isEmpty());

        var current = fixture.service.findRequest(first.requestId()).orElseThrow();
        var cancelled = fixture.service.cancel(new CancelExecutionRequest(
                first.requestId(), current.revision(), fixture.context));

        assertEquals(ExecutionState.CANCELLED, cancelled.state());
        assertEquals(1, fixture.adapter.cancellations);
        assertEquals(ExecutionState.CANCELLED,
                fixture.service.findInstance(instance.instanceId()).orElseThrow().state());
        assertFalse(fixture.service.dispatchNext(fixture.queueId, fixture.context).isEmpty());
    }

    @Test
    void exposesPortableLogsResultsAndSqlExplain() {
        Fixture fixture = new Fixture(1);
        fixture.service.submit(fixture.submission("select 1 as value"));
        var instance = fixture.service.dispatchNext(
                fixture.queueId, fixture.context).orElseThrow();

        var logs = fixture.service.read(new ReadExecutionLogRequest(
                instance.instanceId(), -1, 10, fixture.context));
        var result = fixture.service.read(new ReadExecutionResultRequest(
                instance.instanceId(), 0, 10, fixture.context));
        var plan = fixture.service.explain(new ExplainExecutionRequest(
                fixture.accountId,
                new ExecutionWorkload(
                        ExecutionWorkloadType.SQL, "select 1", Map.of(), Map.of()),
                fixture.context));

        assertEquals("completed", logs.entries().getFirst().message());
        assertEquals(new ExecutionValue.DecimalValue(1), result.rows().getFirst().getFirst());
        assertEquals("scan constants", plan.content());
    }

    private static final class Fixture {
        private final SystemClock clock = new SystemClock();
        private final RequestContext context = RequestContext.system("request-1", clock.now());
        private final ExecutionQueueId queueId = new ExecutionQueueId("default");
        private final ExecutionAccountId accountId = new ExecutionAccountId("local");
        private final LifecycleAdapter adapter = new LifecycleAdapter();
        private final List<DomainEvent> events = new ArrayList<>();
        private final DefaultExecutionService service;

        private Fixture(int maxConcurrency) {
            var queueStore = new InMemoryExecutionQueueStore();
            var accountStore = new InMemoryExecutionAccountStore();
            var control = new DefaultExecutionControlService(queueStore, accountStore, clock);
            control.create(new CreateExecutionQueueRequest(
                    queueId, "Default", maxConcurrency, 0, Map.of(), context));
            control.register(new RegisterExecutionAccountRequest(
                    accountId, "Local", "lifecycle-fixture", "local-binding",
                    Set.of(), Map.of(), context));
            var registry = new InMemoryAdapterRegistry();
            registry.register(adapter);
            service = new DefaultExecutionService(
                    new InMemoryExecutionStore(),
                    queueStore,
                    accountStore,
                    registry,
                    new DirectInvocationExecutor(),
                    new DefaultIntegrationErrorMapper(),
                    clock,
                    new UuidIdGenerator(),
                    events::add,
                    Duration.ofSeconds(30)
            );
        }

        private SubmitExecutionRequest submission(String payload) {
            return new SubmitExecutionRequest(
                    queueId,
                    accountId,
                    new ExecutionWorkload(
                            ExecutionWorkloadType.SQL,
                            payload,
                            Map.of("limit", new ExecutionValue.DecimalValue(1)),
                            Map.of()
                    ),
                    ExecutionResultMode.PAGED,
                    100,
                    context
            );
        }
    }

    private static final class DirectInvocationExecutor implements AdapterInvocationExecutor {
        @Override
        public <T> T execute(
                AdapterRequestContext context,
                IntegrationAdapter adapter,
                String operation,
                Supplier<T> invocation
        ) {
            return invocation.get();
        }
    }

    private static final class LifecycleAdapter implements SqlEngineAdapter {
        private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
                "lifecycle-fixture",
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
        private int submissions;
        private int cancellations;
        private ComputeJobState state = ComputeJobState.RUNNING;

        @Override
        public ComputeJobHandle submit(
                AdapterRequestContext context,
                ComputeJobRequest request
        ) {
            submissions++;
            return new ComputeJobHandle(
                    DESCRIPTOR.adapterId(), request.bindingId(), "job-" + submissions);
        }

        @Override
        public ComputeJobStatus status(
                AdapterRequestContext context,
                ComputeJobHandle handle
        ) {
            return new ComputeJobStatus(handle, state, Instant.now(), "", Map.of());
        }

        @Override
        public void cancel(AdapterRequestContext context, ComputeJobHandle handle) {
            cancellations++;
            state = ComputeJobState.CANCELLED;
        }

        @Override
        public ComputeJobLogPage readLogs(
                AdapterRequestContext context,
                ComputeJobHandle handle,
                long afterSequence,
                int limit
        ) {
            return new ComputeJobLogPage(handle, List.of(
                    new ComputeJobLogEntry(
                            afterSequence + 1, Instant.now(), "INFO", "completed", Map.of())
            ), afterSequence + 2, true);
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
                    List.of(new ComputeResultColumn("value", "bigint", false, Map.of())),
                    List.of(List.of(
                            new com.datausher.integration.runtime.api.IntegrationValue.DecimalValue(1)
                    )),
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
            return new SqlExplainPlan("text", "scan constants", Map.of());
        }

        @Override
        public AdapterDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public AdapterHealth checkHealth() {
            return new AdapterHealth(
                    DESCRIPTOR.adapterId(), AdapterHealthStatus.UP,
                    Instant.now(), "ready", Map.of());
        }
    }
}
