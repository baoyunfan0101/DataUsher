package com.datausher.integration.compute.local;

import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobState;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.contract.ComputeEngineAdapterContract;
import com.datausher.integration.contract.SqlEngineAdapterContract;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSqlEngineAdapterTest {
    @Test
    void satisfiesManagedJobAndExplainContracts() {
        try (var adapter = adapter("contract")) {
            var context = context();
            var request = new ComputeJobRequest(
                    "local-binding", "sql", "select 1 as result_value",
                    Map.of(), Map.of("maxRows", "100"));

            var handle = ComputeEngineAdapterContract.verifyManagedJob(
                    adapter, context, request, Set.of("secret-value"));
            var plan = SqlEngineAdapterContract.verifyExplain(
                    adapter,
                    context,
                    new SqlExecutionRequest(
                            request.bindingId(), request.payload(), Map.of(), 100, Map.of())
            );

            assertEquals(ComputeJobState.SUCCEEDED,
                    adapter.status(context, handle).state());
            assertFalse(plan.content().isBlank());
        }
    }

    @Test
    void bindsTypedParametersAndPagesResults() {
        try (var adapter = adapter("paging")) {
            var context = context();
            var handle = adapter.submit(context, new ComputeJobRequest(
                    "local-binding",
                    "sql",
                    "select x + ? as result_value from system_range(1, 5) order by x",
                    Map.of("1", new IntegrationValue.DecimalValue(10)),
                    Map.of("maxRows", "100")
            ));
            awaitSuccess(adapter, context, handle);

            var first = adapter.readResult(context, handle, 0, 2);
            var second = adapter.readResult(context, handle, 2, 2);

            assertEquals(new IntegrationValue.DecimalValue(11), first.rows().getFirst().getFirst());
            assertTrue(first.hasMore());
            assertEquals(new IntegrationValue.DecimalValue(13), second.rows().getFirst().getFirst());
            var firstLogPage = adapter.readLogs(context, handle, -1, 1);
            assertFalse(firstLogPage.complete());
            assertTrue(adapter.readLogs(
                    context, handle, firstLogPage.nextSequence() - 1, 100).complete());
        }
    }

    private static LocalSqlEngineAdapter adapter(String databaseName) {
        String url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
        return new LocalSqlEngineAdapter(
                "local-sql-" + databaseName,
                ignored -> DriverManager.getConnection(url)
        );
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext(
                "request-1", Instant.now().plusSeconds(30), Map.of());
    }

    private static void awaitSuccess(
            LocalSqlEngineAdapter adapter,
            AdapterRequestContext context,
            com.datausher.integration.compute.api.ComputeJobHandle handle
    ) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        var status = adapter.status(context, handle);
        while (!status.terminal() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            status = adapter.status(context, handle);
        }
        assertEquals(ComputeJobState.SUCCEEDED, status.state(), status.details().toString());
    }
}
