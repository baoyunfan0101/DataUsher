package com.datausher.integration.compute.spark;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobResultPage;
import com.datausher.integration.compute.api.ComputeJobState;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.compute.api.SqlExplainPlan;
import com.datausher.integration.contract.ComputeEngineAdapterContract;
import com.datausher.integration.contract.SqlEngineAdapterContract;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkSqlEngineAdapterTest {
    @Test
    void executesSparkSqlThroughJdbcLifecycle() throws Exception {
        SparkSqlEngineAdapter adapter = adapter();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:spark_sql;DB_CLOSE_DELAY=-1")) {
            connection.createStatement().execute("create table daily_sales(id bigint, amount decimal(10, 2))");
            connection.createStatement().execute("insert into daily_sales values (1, 12.50), (2, 20.00)");
        }
        SqlExecutionRequest request = new SqlExecutionRequest(
                "spark-prod",
                "select id, amount from daily_sales where id >= ? order by id",
                Map.of("1", new IntegrationValue.DecimalValue(1)),
                10,
                Map.of());

        var handle = adapter.submitSql(context(), request);
        var status = adapter.status(context(), handle);
        ComputeJobResultPage result = adapter.readResult(context(), handle, 0, 10);

        assertEquals(ComputeJobState.SUCCEEDED, status.state());
        assertEquals(2, result.rows().size());
        assertEquals("ID", result.columns().getFirst().name());
        assertEquals("12.50", ((IntegrationValue.DecimalValue) result.rows().getFirst().get(1)).value().toPlainString());
        assertFalse(adapter.readLogs(context(), handle, 0, 10).entries().isEmpty());
    }

    @Test
    void explainsSqlAndDeclaresPortableCapabilities() throws Exception {
        SparkSqlEngineAdapter adapter = adapter();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:spark_sql;DB_CLOSE_DELAY=-1")) {
            connection.createStatement().execute("create table if not exists explain_source(id bigint)");
        }
        SqlExecutionRequest request = new SqlExecutionRequest(
                "spark-prod", "select * from explain_source", Map.of(), 10, Map.of());

        SqlExplainPlan explain = adapter.explain(context(), request);

        assertEquals(AdapterType.COMPUTE_ENGINE, adapter.descriptor().type());
        assertTrue(adapter.descriptor().supports(ComputeCapabilities.SQL_EXECUTION));
        assertTrue(adapter.descriptor().supports(ComputeCapabilities.SQL_EXPLAIN));
        assertEquals("text", explain.format());
        assertFalse(explain.content().isBlank());
    }

    @Test
    void satisfiesComputeAndSqlContractFixtures() {
        SparkSqlEngineAdapter adapter = adapter();
        ComputeJobRequest request = new ComputeJobRequest(
                "spark-prod", "sql", "select 1 as id", Map.of(), Map.of("maxRows", "10"));

        ComputeEngineAdapterContract.verifyManagedJob(adapter, context(), request, Set.of("secret"));
        SqlEngineAdapterContract.verifyExplain(adapter, context(), new SqlExecutionRequest(
                "spark-prod", "select 1 as id", Map.of(), 10, Map.of()));
    }

    private static SparkSqlEngineAdapter adapter() {
        return new SparkSqlEngineAdapter(
                request -> DriverManager.getConnection("jdbc:h2:mem:spark_sql;DB_CLOSE_DELAY=-1"));
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext(
                "request-1", Instant.now().plusSeconds(60), Map.of());
    }
}
