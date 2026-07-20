package com.datausher.integration.datasource.hive;

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

class HiveDatasourceConnectorTest {
    @Test
    void exposesHiveAsOfflineStorageDatasource() throws Exception {
        HiveDatasourceConnector connector = connector();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:hive_adapter;DB_CLOSE_DELAY=-1")) {
            connection.createStatement().execute("create table if not exists ods_orders(id bigint)");
            connection.createStatement().execute("delete from ods_orders");
            connection.createStatement().execute("insert into ods_orders values (1)");
        }

        var result = connector.executeQuery(context(), new QueryRequest(
                connection(), "select id from ods_orders", List.of(), 10));

        assertEquals(HiveDatasourceConnector.ADAPTER_ID, connector.descriptor().adapterId());
        assertEquals("offline-storage", connector.descriptor().attributes().get("role"));
        assertTrue(connector.descriptor().supports(DatasourceCapabilities.RELATIONAL_QUERY));
        assertEquals(1, result.rows().size());
    }

    @Test
    void satisfiesDatasourceConnectorContract() {
        DatasourceConnectorContract.verify(connector(), Set.of("secret"));
    }

    private static HiveDatasourceConnector connector() {
        return new HiveDatasourceConnector(
                connection -> DriverManager.getConnection("jdbc:h2:mem:hive_adapter;DB_CLOSE_DELAY=-1"));
    }

    private static DatasourceConnection connection() {
        return new DatasourceConnection("hive-prod", Map.of());
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext("request-1", Instant.now().plusSeconds(60), Map.of());
    }
}
