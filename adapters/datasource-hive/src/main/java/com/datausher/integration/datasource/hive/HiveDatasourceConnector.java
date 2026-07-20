package com.datausher.integration.datasource.hive;

import com.datausher.integration.datasource.jdbc.JdbcRelationalAdapterConfig;
import com.datausher.integration.datasource.jdbc.JdbcRelationalDatasourceConnector;

import java.time.Clock;
import java.util.Map;
import java.util.function.LongSupplier;

public final class HiveDatasourceConnector extends JdbcRelationalDatasourceConnector {
    public static final String ADAPTER_ID = "hive";

    public HiveDatasourceConnector(HiveJdbcConnectionFactory connectionFactory) {
        super(config(), connectionFactory);
    }

    public HiveDatasourceConnector(
            HiveJdbcConnectionFactory connectionFactory,
            Clock clock,
            LongSupplier nanoTime
    ) {
        super(config(), connectionFactory, clock, nanoTime);
    }

    private static JdbcRelationalAdapterConfig config() {
        return new JdbcRelationalAdapterConfig(
                ADAPTER_ID,
                "hive",
                "1.0.0",
                Map.of("role", "offline-storage", "protocol", "jdbc")
        );
    }
}
