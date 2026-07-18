package com.datausher.execution.sql.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlContractTest {
    @Test
    void createsTypedSqlWorkloadsWithoutChangingExecutionCore() {
        var workload = SqlWorkloads.statement(
                "select ?",
                Map.of("1", new ExecutionValue.DecimalValue(1)),
                Map.of()
        );
        var request = new SqlExplainRequest(
                new ExecutionAccountId("local"),
                workload.payload(),
                workload.parameters(),
                workload.options(),
                RequestContext.system("request-1", Instant.EPOCH)
        );

        assertEquals("sql", workload.type().value());
        assertEquals("select ?", request.statement());
        assertThrows(IllegalArgumentException.class,
                () -> SqlWorkloads.statement(" ", Map.of(), Map.of()));
    }
}
