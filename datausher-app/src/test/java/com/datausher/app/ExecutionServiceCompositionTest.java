package com.datausher.app;

import com.datausher.execution.api.CreateExecutionQueueRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ReadExecutionResultRequest;
import com.datausher.execution.api.RegisterExecutionAccountRequest;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.execution.core.DefaultExecutionControlService;
import com.datausher.execution.core.DefaultExecutionService;
import com.datausher.execution.core.InMemoryExecutionAccountStore;
import com.datausher.execution.core.InMemoryExecutionQueueStore;
import com.datausher.execution.core.InMemoryExecutionStore;
import com.datausher.execution.sql.api.SqlExplainRequest;
import com.datausher.execution.sql.api.SqlWorkloads;
import com.datausher.execution.sql.core.DefaultSqlExplainService;
import com.datausher.integration.compute.local.LocalSqlEngineAdapter;
import com.datausher.integration.runtime.core.DeadlineEnforcingAdapterInvocationExecutor;
import com.datausher.integration.runtime.core.DefaultIntegrationErrorMapper;
import com.datausher.integration.runtime.core.InMemoryAdapterRegistry;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExecutionServiceCompositionTest {
    @Test
    void executesSqlThroughReplaceableExecutionAndAdapterBoundaries() {
        String url = "jdbc:h2:mem:composition;DB_CLOSE_DELAY=-1";
        try (var adapter = new LocalSqlEngineAdapter(
                "local-sql", ignored -> DriverManager.getConnection(url));
             var invocationThreads = Executors.newVirtualThreadPerTaskExecutor()) {
            var clock = new SystemClock();
            var context = RequestContext.system("request-1", clock.now());
            var queueStore = new InMemoryExecutionQueueStore();
            var accountStore = new InMemoryExecutionAccountStore();
            var control = new DefaultExecutionControlService(
                    queueStore, accountStore, clock);
            var queue = control.create(new CreateExecutionQueueRequest(
                    new com.datausher.execution.api.ExecutionQueueId("interactive"),
                    "Interactive", 2, 100, Map.of(), context));
            var account = control.register(new RegisterExecutionAccountRequest(
                    new ExecutionAccountId("local"),
                    "Local SQL",
                    adapter.descriptor().adapterId(),
                    "local-binding",
                    Set.of(SqlWorkloads.TYPE),
                    Map.of(),
                    context
            ));
            var registry = new InMemoryAdapterRegistry();
            registry.register(adapter);
            var invocationExecutor = new DeadlineEnforcingAdapterInvocationExecutor(
                    invocationThreads, java.time.Clock.systemUTC());
            var service = new DefaultExecutionService(
                    new InMemoryExecutionStore(),
                    queueStore,
                    accountStore,
                    registry,
                    invocationExecutor,
                    new DefaultIntegrationErrorMapper(),
                    clock,
                    new UuidIdGenerator(),
                    event -> { },
                    Duration.ofSeconds(30)
            );
            var sqlExplainService = new DefaultSqlExplainService(
                    control, registry, invocationExecutor, clock, Duration.ofSeconds(30));
            var workload = SqlWorkloads.statement(
                    "select x as result_value from system_range(1, 3) order by x",
                    Map.of(),
                    Map.of("maxRows", "100")
            );

            service.submit(new SubmitExecutionRequest(
                    queue.queueId(), account.accountId(), workload,
                    ExecutionResultMode.PAGED, 100, context));
            var instance = service.dispatchNext(queue.queueId(), context).orElseThrow();
            instance = awaitTerminal(service, instance, context);
            var result = service.read(new ReadExecutionResultRequest(
                    instance.instanceId(), 0, 2, context));
            var plan = sqlExplainService.explain(new SqlExplainRequest(
                    account.accountId(), workload.payload(), workload.parameters(),
                    workload.options(), context));

            assertEquals(ExecutionState.SUCCEEDED, instance.state());
            assertEquals(new ExecutionValue.DecimalValue(1),
                    result.rows().getFirst().getFirst());
            assertEquals(2, result.rows().size());
            assertFalse(plan.content().isBlank());
        }
    }

    private static com.datausher.execution.api.ExecutionInstance awaitTerminal(
            DefaultExecutionService service,
            com.datausher.execution.api.ExecutionInstance initial,
            RequestContext context
    ) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        var current = initial;
        while (!current.state().terminal() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            current = service.refresh(current.instanceId(), context);
        }
        return current;
    }
}
