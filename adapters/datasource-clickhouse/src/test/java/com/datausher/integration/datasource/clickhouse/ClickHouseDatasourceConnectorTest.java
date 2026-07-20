package com.datausher.integration.datasource.clickhouse;

import com.datausher.integration.contract.DatasourceConnectorContract;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.QueryRequest;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickHouseDatasourceConnectorTest {
    @Test
    void exposesClickHouseAsDashboardServingDatasource() throws Exception {
        ClickHouseDatasourceConnector connector = connector();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:clickhouse_adapter;DB_CLOSE_DELAY=-1")) {
            connection.createStatement().execute("create table if not exists ads_daily_sales(metric_day varchar(10), amount decimal(10, 2))");
            connection.createStatement().execute("delete from ads_daily_sales");
            connection.createStatement().execute("insert into ads_daily_sales values ('2026-07-20', 12.50)");
        }

        var result = connector.executeQuery(context(), new QueryRequest(
                connection(), "select metric_day, amount from ads_daily_sales", List.of(), 10));

        assertEquals(ClickHouseDatasourceConnector.ADAPTER_ID, connector.descriptor().adapterId());
        assertEquals("dashboard-serving", connector.descriptor().attributes().get("role"));
        assertTrue(connector.descriptor().supports(DatasourceCapabilities.RELATIONAL_QUERY));
        assertEquals(1, result.rows().size());
    }

    @Test
    void satisfiesDatasourceConnectorContract() {
        DatasourceConnectorContract.verify(connector(), Set.of("secret"));
    }

    private static ClickHouseDatasourceConnector connector() {
        return new ClickHouseDatasourceConnector(
                connection -> DriverManager.getConnection("jdbc:h2:mem:clickhouse_adapter;DB_CLOSE_DELAY=-1"));
    }

    private static DatasourceConnection connection() {
        return new DatasourceConnection("clickhouse-prod", Map.of());
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext("request-1", Instant.now().plusSeconds(60), Map.of());
    }
}
