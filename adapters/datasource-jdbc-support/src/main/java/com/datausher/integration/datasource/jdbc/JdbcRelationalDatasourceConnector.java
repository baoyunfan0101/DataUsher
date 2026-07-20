package com.datausher.integration.datasource.jdbc;

import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DatasourceObjectAttributes;
import com.datausher.integration.datasource.api.DatasourceObjectKinds;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.datasource.api.QueryRequest;
import com.datausher.integration.datasource.api.QueryResult;
import com.datausher.integration.datasource.api.RelationalDatasourceConnector;
import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

public class JdbcRelationalDatasourceConnector implements RelationalDatasourceConnector {
    private static final int MAX_ALLOWED_ROWS = 100_000;

    private final JdbcRelationalAdapterConfig config;
    private final AdapterDescriptor descriptor;
    private final JdbcRelationalConnectionFactory connectionFactory;
    private final Clock clock;
    private final LongSupplier nanoTime;

    public JdbcRelationalDatasourceConnector(
            JdbcRelationalAdapterConfig config,
            JdbcRelationalConnectionFactory connectionFactory
    ) {
        this(config, connectionFactory, Clock.systemUTC(), System::nanoTime);
    }

    public JdbcRelationalDatasourceConnector(
            JdbcRelationalAdapterConfig config,
            JdbcRelationalConnectionFactory connectionFactory,
            Clock clock,
            LongSupplier nanoTime
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
        Map<String, String> attributes = new LinkedHashMap<>(config.attributes());
        attributes.putIfAbsent("protocol", "jdbc");
        attributes.putIfAbsent("vendor", config.vendor());
        this.descriptor = new AdapterDescriptor(
                config.adapterId(),
                AdapterType.DATASOURCE,
                config.version(),
                Set.of(
                        AdapterCapability.of(DatasourceCapabilities.DISCOVERY),
                        AdapterCapability.of(DatasourceCapabilities.RELATIONAL_QUERY)
                ),
                attributes
        );
    }

    @Override
    public AdapterDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public AdapterHealth checkHealth() {
        return new AdapterHealth(
                descriptor.adapterId(), AdapterHealthStatus.UP, clock.instant(),
                "adapter ready", Map.of("credentialHandling", "external"));
    }

    @Override
    public ConnectionTestResult testConnection(
            AdapterRequestContext context,
            DatasourceConnection connection
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(connection, "connection must not be null");
        checkDeadline(context, "connection test");
        long startedAt = nanoTime.getAsLong();
        try (Connection jdbcConnection = connectionFactory.open(connection)) {
            boolean valid = jdbcConnection.isValid(1);
            Duration latency = elapsed(startedAt);
            return new ConnectionTestResult(
                    valid,
                    latency,
                    valid ? config.vendor() + " connection is valid"
                            : config.vendor() + " connection validation failed",
                    Map.of("adapterId", descriptor.adapterId())
            );
        } catch (SQLException exception) {
            return new ConnectionTestResult(
                    false,
                    elapsed(startedAt),
                    config.vendor() + " connection failed",
                    Map.of("sqlState", safe(exception.getSQLState()))
            );
        }
    }

    @Override
    public List<DatasourceObject> discover(
            AdapterRequestContext context,
            DiscoveryRequest request
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        checkDeadline(context, "discovery");
        try (Connection connection = connectionFactory.open(request.connection())) {
            DatabaseMetaData metadata = connection.getMetaData();
            String namespace = request.namespace().isBlank() ? currentNamespace(connection) : request.namespace();
            List<DatasourceObject> objects = new ArrayList<>();
            objects.addAll(discoverCatalogs(metadata));
            objects.addAll(discoverTables(metadata, namespace));
            objects.addAll(discoverColumns(metadata, namespace));
            return List.copyOf(objects);
        } catch (SQLException exception) {
            throw mapSqlException("discovery", exception);
        }
    }

    @Override
    public QueryResult executeQuery(AdapterRequestContext context, QueryRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        checkDeadline(context, "query");
        int maxRows = Math.min(request.maxRows(), MAX_ALLOWED_ROWS);
        try (Connection connection = connectionFactory.open(request.connection());
             PreparedStatement statement = connection.prepareStatement(request.statement())) {
            bind(statement, request.parameters());
            statement.setMaxRows(maxRows + 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readRows(resultSet, maxRows);
            }
        } catch (SQLException exception) {
            throw mapSqlException("query", exception);
        }
    }

    private List<DatasourceObject> discoverCatalogs(DatabaseMetaData metadata) throws SQLException {
        List<DatasourceObject> catalogs = new ArrayList<>();
        try (ResultSet resultSet = metadata.getCatalogs()) {
            while (resultSet.next()) {
                String catalog = resultSet.getString("TABLE_CAT");
                catalogs.add(new DatasourceObject(
                        catalog,
                        DatasourceObjectKinds.DATABASE,
                        Map.of(
                                DatasourceObjectAttributes.EXTERNAL_ID, catalog,
                                DatasourceObjectAttributes.QUALIFIED_NAME, catalog
                        )));
            }
        }
        return catalogs;
    }

    private List<DatasourceObject> discoverTables(
            DatabaseMetaData metadata,
            String namespace
    ) throws SQLException {
        List<DatasourceObject> tables = new ArrayList<>();
        try (ResultSet resultSet = metadata.getTables(namespace, null, "%", tableTypes())) {
            while (resultSet.next()) {
                String table = resultSet.getString("TABLE_NAME");
                String tableType = resultSet.getString("TABLE_TYPE");
                String qualifiedName = namespace.isBlank() ? table : namespace + "." + table;
                tables.add(new DatasourceObject(
                        table,
                        DatasourceObjectKinds.TABLE,
                        attributes(
                                DatasourceObjectAttributes.EXTERNAL_ID, qualifiedName,
                                DatasourceObjectAttributes.PARENT_EXTERNAL_ID, namespace,
                                DatasourceObjectAttributes.QUALIFIED_NAME, qualifiedName,
                                DatasourceObjectAttributes.DATABASE, namespace,
                                DatasourceObjectAttributes.TABLE, table,
                                DatasourceObjectAttributes.TABLE_TYPE, safe(tableType),
                                DatasourceObjectAttributes.REMARKS, safe(resultSet.getString("REMARKS"))
                        )));
            }
        }
        return tables;
    }

    private List<DatasourceObject> discoverColumns(
            DatabaseMetaData metadata,
            String namespace
    ) throws SQLException {
        List<DatasourceObject> columns = new ArrayList<>();
        try (ResultSet resultSet = metadata.getColumns(namespace, null, "%", "%")) {
            while (resultSet.next()) {
                String table = resultSet.getString("TABLE_NAME");
                String column = resultSet.getString("COLUMN_NAME");
                String tableQualifiedName = namespace.isBlank() ? table : namespace + "." + table;
                String columnQualifiedName = tableQualifiedName + "." + column;
                columns.add(new DatasourceObject(
                        column,
                        DatasourceObjectKinds.COLUMN,
                        attributes(
                                DatasourceObjectAttributes.EXTERNAL_ID, columnQualifiedName,
                                DatasourceObjectAttributes.PARENT_EXTERNAL_ID, tableQualifiedName,
                                DatasourceObjectAttributes.QUALIFIED_NAME, columnQualifiedName,
                                DatasourceObjectAttributes.DATABASE, namespace,
                                DatasourceObjectAttributes.TABLE, table,
                                DatasourceObjectAttributes.DATA_TYPE, safe(resultSet.getString("TYPE_NAME")),
                                DatasourceObjectAttributes.ORDINAL_POSITION,
                                Integer.toString(resultSet.getInt("ORDINAL_POSITION")),
                                DatasourceObjectAttributes.NULLABLE,
                                Boolean.toString(resultSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls),
                                DatasourceObjectAttributes.REMARKS, safe(resultSet.getString("REMARKS"))
                        )));
            }
        }
        return columns;
    }

    protected String[] tableTypes() {
        return new String[] {"TABLE", "VIEW"};
    }

    private String currentNamespace(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        return catalog == null ? "" : catalog;
    }

    private static QueryResult readRows(ResultSet resultSet, int maxRows) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int index = 1; index <= columnCount; index++) {
            String name = metadata.getColumnLabel(index);
            if (name == null || name.isBlank()) {
                name = metadata.getColumnName(index);
            }
            columns.add(name);
        }
        List<List<IntegrationValue>> rows = new ArrayList<>();
        boolean truncated = false;
        while (resultSet.next()) {
            if (rows.size() >= maxRows) {
                truncated = true;
                break;
            }
            List<IntegrationValue> row = new ArrayList<>();
            for (int index = 1; index <= columnCount; index++) {
                row.add(toValue(resultSet.getObject(index)));
            }
            rows.add(List.copyOf(row));
        }
        return new QueryResult(List.copyOf(columns), List.copyOf(rows), truncated);
    }

    private static void bind(
            PreparedStatement statement,
            List<IntegrationValue> parameters
    ) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            bind(statement, index + 1, parameters.get(index));
        }
    }

    private static void bind(PreparedStatement statement, int index, IntegrationValue value)
            throws SQLException {
        switch (value) {
            case IntegrationValue.NullValue ignored -> statement.setObject(index, null);
            case IntegrationValue.TextValue text -> statement.setString(index, text.value());
            case IntegrationValue.BooleanValue bool -> statement.setBoolean(index, bool.value());
            case IntegrationValue.DecimalValue decimal -> statement.setBigDecimal(index, decimal.value());
            case IntegrationValue.BinaryValue binary -> statement.setBytes(index, binary.bytes());
            case IntegrationValue.InstantValue instant -> statement.setTimestamp(index, Timestamp.from(instant.value()));
            case IntegrationValue.DateValue date -> statement.setDate(index, Date.valueOf(date.value()));
            case IntegrationValue.DateTimeValue dateTime -> statement.setTimestamp(index, Timestamp.valueOf(dateTime.value()));
            case IntegrationValue.ArrayValue array -> statement.setString(index, array.values().toString());
            case IntegrationValue.ObjectValue object -> statement.setString(index, object.values().toString());
        }
    }

    private static IntegrationValue toValue(Object value) {
        return switch (value) {
            case null -> new IntegrationValue.NullValue();
            case String text -> new IntegrationValue.TextValue(text);
            case Boolean bool -> new IntegrationValue.BooleanValue(bool);
            case BigDecimal decimal -> new IntegrationValue.DecimalValue(decimal);
            case Integer number -> new IntegrationValue.DecimalValue(number.longValue());
            case Long number -> new IntegrationValue.DecimalValue(number);
            case Short number -> new IntegrationValue.DecimalValue(number.longValue());
            case Float number -> new IntegrationValue.DecimalValue(number.doubleValue());
            case Double number -> new IntegrationValue.DecimalValue(number);
            case Date date -> new IntegrationValue.DateValue(date.toLocalDate());
            case Timestamp timestamp -> new IntegrationValue.DateTimeValue(timestamp.toLocalDateTime());
            case LocalDate date -> new IntegrationValue.DateValue(date);
            case LocalDateTime dateTime -> new IntegrationValue.DateTimeValue(dateTime);
            case Instant instant -> new IntegrationValue.InstantValue(instant);
            default -> new IntegrationValue.TextValue(value.toString());
        };
    }

    private void checkDeadline(AdapterRequestContext context, String operation) {
        if (context.isExpired(clock)) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.TIMEOUT,
                    config.vendor() + " " + operation + " request expired before adapter execution",
                    true,
                    Map.of("adapterId", descriptor.adapterId()),
                    null
            );
        }
    }

    private ExternalSystemException mapSqlException(String operation, SQLException exception) {
        return new ExternalSystemException(
                IntegrationErrorCode.EXTERNAL_FAILURE,
                config.vendor() + " " + operation + " failed",
                false,
                Map.of("sqlState", safe(exception.getSQLState())),
                exception
        );
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - startedAt));
    }

    private static Map<String, String> attributes(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("attributes require key/value pairs");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            if (values[index + 1] != null && !values[index + 1].isBlank()) {
                attributes.put(values[index], values[index + 1]);
            }
        }
        return Map.copyOf(attributes);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
