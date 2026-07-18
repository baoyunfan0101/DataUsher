package com.datausher.integration.datasource.mysql;

import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceConnector;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DatasourceObjectAttributes;
import com.datausher.integration.datasource.api.DatasourceObjectKinds;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.datasource.api.QueryRequest;
import com.datausher.integration.datasource.api.QueryResult;
import com.datausher.integration.datasource.contract.DatasourceConnectorContract;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlDatasourceConnectorTest extends DatasourceConnectorContract {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
    private static final DatasourceConnection CONNECTION = new DatasourceConnection(
            "analytics-credential",
            Map.of("ssl.mode", "required")
    );

    private final MySqlDatasourceConnector connector = connector(
            connection -> JdbcTestDoubles.connection());

    @Override
    protected DatasourceConnector connector() {
        return connector;
    }

    @Override
    protected DatasourceConnection validConnection() {
        return CONNECTION;
    }

    @Test
    void discoversStableHierarchyAndColumnTypes() {
        List<DatasourceObject> objects = connector.discover(
                requestContext(),
                new DiscoveryRequest(CONNECTION, "", Map.of("includeViews", "true"))
        );
        DatasourceObject column = objects.stream()
                .filter(object -> object.kind().equals(DatasourceObjectKinds.COLUMN))
                .filter(object -> object.name().equals("id"))
                .findFirst()
                .orElseThrow();

        assertEquals("mysql.column/analytics/orders/id", column.attributes().get(
                DatasourceObjectAttributes.EXTERNAL_ID));
        assertEquals("mysql.table/analytics/orders", column.attributes().get(
                DatasourceObjectAttributes.PARENT_EXTERNAL_ID));
        assertEquals("BIGINT", column.attributes().get(
                DatasourceObjectAttributes.DATA_TYPE));
        assertEquals("analytics.orders.id", column.attributes().get(
                DatasourceObjectAttributes.QUALIFIED_NAME));
    }

    @Test
    void mapsJdbcRowsAndReportsTruncation() {
        QueryResult result = connector.executeQuery(
                requestContext(),
                new QueryRequest(CONNECTION, "select id, name from orders", List.of(), 1)
        );

        assertEquals(List.of("id", "name"), result.columns());
        assertEquals(1, result.rows().size());
        assertTrue(result.truncated());
        assertInstanceOf(IntegrationValue.DecimalValue.class, result.rows().getFirst().getFirst());
        assertInstanceOf(IntegrationValue.TextValue.class, result.rows().getFirst().get(1));
    }

    @Test
    void returnsSanitizedConnectionFailures() {
        MySqlDatasourceConnector failing = connector(connection -> {
            throw new SQLException("password=do-not-expose", "28000", 1045);
        });

        ConnectionTestResult result = failing.testConnection(requestContext(), CONNECTION);

        assertFalse(result.successful());
        assertFalse(result.message().contains("do-not-expose"));
        assertEquals("28000", result.details().get("sqlState"));
    }

    @Test
    void rejectsExpiredRequestsBeforeOpeningAConnection() {
        AdapterRequestContext expired = new AdapterRequestContext(
                "expired-request",
                NOW,
                Map.of()
        );

        ExternalSystemException exception = assertThrows(
                ExternalSystemException.class,
                () -> connector.discover(
                        expired,
                        new DiscoveryRequest(CONNECTION, "", Map.of())
                )
        );

        assertEquals(IntegrationErrorCode.TIMEOUT, exception.errorCode());
    }

    private static MySqlDatasourceConnector connector(JdbcConnectionFactory factory) {
        AtomicLong nanoTime = new AtomicLong();
        return new MySqlDatasourceConnector(
                factory,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> nanoTime.getAndAdd(2_000_000)
        );
    }

    private static final class JdbcTestDoubles {
        private JdbcTestDoubles() {
        }

        static Connection connection() {
            return proxy(Connection.class, (method, arguments) -> switch (method.getName()) {
                case "isValid" -> true;
                case "getMetaData" -> metadata();
                case "getCatalog" -> "analytics";
                case "prepareStatement" -> preparedStatement();
                case "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private static DatabaseMetaData metadata() {
            return proxy(DatabaseMetaData.class, (method, arguments) -> switch (method.getName()) {
                case "getCatalogs" -> resultSet(List.of(
                        row("TABLE_CAT", "analytics")
                ));
                case "getTables" -> resultSet(List.of(
                        row(
                                "TABLE_NAME", "orders",
                                "TABLE_TYPE", "TABLE",
                                "REMARKS", "Customer orders"
                        )
                ));
                case "getColumns" -> resultSet(List.of(
                        row(
                                "COLUMN_NAME", "id",
                                "TYPE_NAME", "BIGINT",
                                "ORDINAL_POSITION", 1,
                                "NULLABLE", DatabaseMetaData.columnNoNulls,
                                "REMARKS", "Primary key"
                        ),
                        row(
                                "COLUMN_NAME", "name",
                                "TYPE_NAME", "VARCHAR",
                                "ORDINAL_POSITION", 2,
                                "NULLABLE", DatabaseMetaData.columnNullable,
                                "REMARKS", "Order name"
                        )
                ));
                default -> defaultValue(method.getReturnType());
            });
        }

        private static PreparedStatement preparedStatement() {
            return proxy(PreparedStatement.class, (method, arguments) -> switch (method.getName()) {
                case "executeQuery" -> resultSet(List.of(
                        row("id", 1L, "name", "first"),
                        row("id", 2L, "name", "second")
                ));
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private static ResultSet resultSet(List<LinkedHashMap<String, Object>> rows) {
            AtomicLong cursor = new AtomicLong(-1);
            List<String> labels = rows.isEmpty()
                    ? List.of()
                    : List.copyOf(rows.getFirst().keySet());
            return proxy(ResultSet.class, (method, arguments) -> switch (method.getName()) {
                case "next" -> cursor.incrementAndGet() < rows.size();
                case "getString" -> {
                    Object value = value(rows, labels, cursor, arguments[0]);
                    yield value == null ? null : value.toString();
                }
                case "getInt" -> {
                    Object value = value(rows, labels, cursor, arguments[0]);
                    yield value == null ? 0 : ((Number) value).intValue();
                }
                case "getObject" -> value(rows, labels, cursor, arguments[0]);
                case "getMetaData" -> resultSetMetadata(labels);
                case "close" -> null;
                case "wasNull" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private static ResultSetMetaData resultSetMetadata(List<String> labels) {
            return proxy(ResultSetMetaData.class, (method, arguments) -> switch (method.getName()) {
                case "getColumnCount" -> labels.size();
                case "getColumnLabel" -> labels.get((Integer) arguments[0] - 1);
                default -> defaultValue(method.getReturnType());
            });
        }

        private static Object value(
                List<LinkedHashMap<String, Object>> rows,
                List<String> labels,
                AtomicLong cursor,
                Object key
        ) {
            LinkedHashMap<String, Object> row = rows.get((int) cursor.get());
            if (key instanceof Integer index) {
                return row.get(labels.get(index - 1));
            }
            return row.get(key.toString());
        }

        private static LinkedHashMap<String, Object> row(Object... entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < entries.length; index += 2) {
                row.put(entries[index].toString(), entries[index + 1]);
            }
            return row;
        }

        private static <T> T proxy(Class<T> type, Invocation invocation) {
            return type.cast(Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] {type},
                    (proxy, method, arguments) -> invocation.invoke(
                            method,
                            arguments == null ? new Object[0] : arguments)
            ));
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0F;
            }
            if (type == double.class) {
                return 0D;
            }
            if (type == char.class) {
                return '\0';
            }
            return null;
        }

        @FunctionalInterface
        private interface Invocation {
            Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
        }
    }
}
