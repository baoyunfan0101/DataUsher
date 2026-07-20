package com.datausher.integration.datasource.clickhouse;

import com.datausher.integration.datasource.jdbc.JdbcRelationalAdapterConfig;
import com.datausher.integration.datasource.jdbc.JdbcRelationalDatasourceConnector;

import java.time.Clock;
import java.util.Map;
import java.util.function.LongSupplier;

public final class ClickHouseDatasourceConnector extends JdbcRelationalDatasourceConnector {
    public static final String ADAPTER_ID = "clickhouse";

    public ClickHouseDatasourceConnector(ClickHouseJdbcConnectionFactory connectionFactory) {
        super(config(), connectionFactory);
    }

    public ClickHouseDatasourceConnector(
            ClickHouseJdbcConnectionFactory connectionFactory,
            Clock clock,
            LongSupplier nanoTime
    ) {
        super(config(), connectionFactory, clock, nanoTime);
    }

    private static JdbcRelationalAdapterConfig config() {
        return new JdbcRelationalAdapterConfig(
                ADAPTER_ID,
                "clickhouse",
                "1.0.0",
                Map.of("role", "dashboard-serving", "protocol", "jdbc")
        );
    }
}
