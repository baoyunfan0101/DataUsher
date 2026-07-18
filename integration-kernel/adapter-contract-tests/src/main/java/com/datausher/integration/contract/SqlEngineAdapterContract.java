package com.datausher.integration.contract;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.SqlEngineAdapter;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.compute.api.SqlExplainPlan;
import com.datausher.integration.runtime.api.AdapterRequestContext;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SqlEngineAdapterContract {
    private SqlEngineAdapterContract() {
    }

    public static SqlExplainPlan verifyExplain(
            SqlEngineAdapter adapter,
            AdapterRequestContext context,
            SqlExecutionRequest request
    ) {
        Objects.requireNonNull(adapter, "adapter must not be null");
        assertTrue(adapter.descriptor().supports(ComputeCapabilities.SQL_EXPLAIN),
                "SQL explain requires the SQL explain capability");
        SqlExplainPlan plan = adapter.explain(context, request);
        assertNotNull(plan, "SQL explain must return a plan");
        assertFalse(plan.format().isBlank(), "SQL explain format must not be blank");
        assertFalse(plan.content().isBlank(), "SQL explain content must not be blank");
        return plan;
    }
}
