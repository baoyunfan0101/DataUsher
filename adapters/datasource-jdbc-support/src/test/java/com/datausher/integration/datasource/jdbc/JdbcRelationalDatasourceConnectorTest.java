package com.datausher.integration.datasource.jdbc;

import com.datausher.integration.contract.DatasourceConnectorContract;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.datasource.api.QueryRequest;
import com.datausher.integration.datasource.api.QueryResult;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRelationalDatasourceConnectorTest {
    @Test
    void discoversAndQueriesRelationalObjectsThroughJdbc() throws Exception {
        JdbcRelationalDatasourceConnector connector = connector();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:jdbc_support;DB_CLOSE_DELAY=-1")) {
            connection.createStatement().execute("create table if not exists orders(id bigint, name varchar(32))");
            connection.createStatement().execute("delete from orders");
            connection.createStatement().execute("insert into orders values (1, 'first'), (2, 'second')");
        }

        var objects = connector.discover(context(), new DiscoveryRequest(connection(), "JDBC_SUPPORT", Map.of()));
        QueryResult result = connector.executeQuery(context(), new QueryRequest(
                connection(), "select id, name from orders where id >= ? order by id",
                List.of(new IntegrationValue.DecimalValue(1)), 10));

        assertTrue(connector.descriptor().supports(DatasourceCapabilities.DISCOVERY));
        assertTrue(connector.descriptor().supports(DatasourceCapabilities.RELATIONAL_QUERY));
        assertFalse(objects.isEmpty());
        assertEquals(List.of("ID", "NAME"), result.columns());
        assertEquals(2, result.rows().size());
    }

    @Test
    void satisfiesDatasourceConnectorContract() {
        JdbcRelationalDatasourceConnector connector = connector();

        DatasourceConnectorContract.verify(connector, Set.of("secret"));
    }

    private static JdbcRelationalDatasourceConnector connector() {
        return new JdbcRelationalDatasourceConnector(
                new JdbcRelationalAdapterConfig("jdbc-test", "jdbc", "1.0.0", Map.of()),
                connection -> DriverManager.getConnection("jdbc:h2:mem:jdbc_support;DB_CLOSE_DELAY=-1"));
    }

    private static DatasourceConnection connection() {
        return new DatasourceConnection("warehouse", Map.of());
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext("request-1", Instant.now().plusSeconds(60), Map.of());
    }
}
