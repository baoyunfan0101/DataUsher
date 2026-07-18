package com.datausher.integration.datasource.mysql;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongSupplier;

public final class MySqlDatasourceConnector implements RelationalDatasourceConnector {
    public static final String ADAPTER_ID = "mysql";

    private static final AdapterDescriptor DESCRIPTOR = new AdapterDescriptor(
            ADAPTER_ID,
            AdapterType.DATASOURCE,
            "1.0.0",
            Set.of(
                    AdapterCapability.of(DatasourceCapabilities.DISCOVERY),
                    AdapterCapability.of(DatasourceCapabilities.RELATIONAL_QUERY)
            ),
            Map.of("protocol", "jdbc", "vendor", "mysql")
    );

    private final JdbcConnectionFactory connectionFactory;
    private final Clock clock;
    private final LongSupplier nanoTime;

    public MySqlDatasourceConnector(JdbcConnectionFactory connectionFactory) {
        this(connectionFactory, Clock.systemUTC(), System::nanoTime);
    }

    public MySqlDatasourceConnector(
            JdbcConnectionFactory connectionFactory,
            Clock clock,
            LongSupplier nanoTime
    ) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
    }

    @Override
    public AdapterDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public AdapterHealth checkHealth() {
        return new AdapterHealth(
                ADAPTER_ID,
                AdapterHealthStatus.UP,
                clock.instant(),
                "adapter ready",
                Map.of("credentialHandling", "external")
        );
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
            boolean valid = jdbcConnection.isValid(validationTimeoutSeconds(context));
            Duration latency = elapsed(startedAt);
            if (!valid) {
                return new ConnectionTestResult(
                        false,
                        latency,
                        "MySQL connection validation failed",
                        Map.of("adapterId", ADAPTER_ID)
                );
            }
            return new ConnectionTestResult(
                    true,
                    latency,
                    "MySQL connection is valid",
                    Map.of("adapterId", ADAPTER_ID)
            );
        } catch (SQLException exception) {
            return new ConnectionTestResult(
                    false,
                    elapsed(startedAt),
                    "MySQL connection test failed",
                    errorDetails(exception, "connection-test")
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
        checkDeadline(context, "metadata discovery");
        try (Connection connection = connectionFactory.open(request.connection())) {
            DatabaseMetaData metadata = connection.getMetaData();
            List<DatasourceObject> objects = new ArrayList<>();
            for (String database : databases(connection, metadata, request.namespace())) {
                checkDeadline(context, "metadata discovery");
                String databaseExternalId = externalId("database", database);
                objects.add(databaseObject(database, databaseExternalId));
                discoverTables(
                        metadata,
                        database,
                        databaseExternalId,
                        includeViews(request.options()),
                        context,
                        objects
                );
            }
            return List.copyOf(objects);
        } catch (SQLException exception) {
            throw externalFailure("metadata discovery", exception);
        }
    }

    @Override
    public QueryResult executeQuery(AdapterRequestContext context, QueryRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        checkDeadline(context, "query execution");
        try (Connection connection = connectionFactory.open(request.connection());
             PreparedStatement statement = connection.prepareStatement(request.statement())) {
            int fetchLimit = request.maxRows() == Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : request.maxRows() + 1;
            statement.setMaxRows(fetchLimit);
            bind(statement, request.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                return readQueryResult(resultSet, request.maxRows());
            }
        } catch (SQLException exception) {
            throw externalFailure("query execution", exception);
        }
    }

    private void discoverTables(
            DatabaseMetaData metadata,
            String database,
            String databaseExternalId,
            boolean includeViews,
            AdapterRequestContext context,
            List<DatasourceObject> objects
    ) throws SQLException {
        String[] tableTypes = includeViews
                ? new String[] {"TABLE", "VIEW"}
                : new String[] {"TABLE"};
        try (ResultSet tableResults = metadata.getTables(
                database, null, "%", tableTypes)) {
            while (tableResults.next()) {
                checkDeadline(context, "metadata discovery");
                String table = tableResults.getString("TABLE_NAME");
                String tableType = tableResults.getString("TABLE_TYPE");
                String remarks = tableResults.getString("REMARKS");
                String tableExternalId = externalId("table", database, table);
                objects.add(tableObject(
                        database,
                        table,
                        tableType,
                        remarks,
                        databaseExternalId,
                        tableExternalId
                ));
                discoverColumns(
                        metadata,
                        database,
                        table,
                        tableExternalId,
                        context,
                        objects
                );
            }
        }
    }

    private void discoverColumns(
            DatabaseMetaData metadata,
            String database,
            String table,
            String tableExternalId,
            AdapterRequestContext context,
            List<DatasourceObject> objects
    ) throws SQLException {
        try (ResultSet columnResults = metadata.getColumns(
                database, null, table, "%")) {
            while (columnResults.next()) {
                checkDeadline(context, "metadata discovery");
                String column = columnResults.getString("COLUMN_NAME");
                String nativeType = columnResults.getString("TYPE_NAME");
                int ordinalPosition = columnResults.getInt("ORDINAL_POSITION");
                int nullableCode = columnResults.getInt("NULLABLE");
                boolean nullable = nullableCode != DatabaseMetaData.columnNoNulls;
                String remarks = columnResults.getString("REMARKS");
                objects.add(columnObject(
                        database,
                        table,
                        column,
                        nativeType,
                        ordinalPosition,
                        nullable,
                        remarks,
                        tableExternalId,
                        externalId("column", database, table, column)
                ));
            }
        }
    }

    private static List<String> databases(
            Connection connection,
            DatabaseMetaData metadata,
            String namespace
    ) throws SQLException {
        TreeSet<String> databases = new TreeSet<>();
        if (!namespace.isBlank()) {
            databases.add(namespace);
            return List.copyOf(databases);
        }
        try (ResultSet catalogs = metadata.getCatalogs()) {
            while (catalogs.next()) {
                String database = catalogs.getString("TABLE_CAT");
                if (database != null && !database.isBlank()) {
                    databases.add(database);
                }
            }
        }
        if (databases.isEmpty()) {
            String currentCatalog = connection.getCatalog();
            if (currentCatalog != null && !currentCatalog.isBlank()) {
                databases.add(currentCatalog);
            }
        }
        return List.copyOf(databases);
    }

    private static DatasourceObject databaseObject(String database, String externalId) {
        return new DatasourceObject(
                database,
                DatasourceObjectKinds.DATABASE,
                Map.of(
                        DatasourceObjectAttributes.EXTERNAL_ID, externalId,
                        DatasourceObjectAttributes.QUALIFIED_NAME, database,
                        DatasourceObjectAttributes.DATABASE, database
                )
        );
    }

    private static DatasourceObject tableObject(
            String database,
            String table,
            String tableType,
            String remarks,
            String parentExternalId,
            String externalId
    ) {
        Map<String, String> attributes = baseAttributes(
                externalId, parentExternalId, qualifiedName(database, table));
        attributes.put(DatasourceObjectAttributes.DATABASE, database);
        attributes.put(DatasourceObjectAttributes.TABLE, table);
        attributes.put(DatasourceObjectAttributes.TABLE_TYPE,
                textOrDefault(tableType, "OTHER"));
        putOptional(attributes, DatasourceObjectAttributes.REMARKS, remarks);
        return new DatasourceObject(table, DatasourceObjectKinds.TABLE, attributes);
    }

    private static DatasourceObject columnObject(
            String database,
            String table,
            String column,
            String nativeType,
            int ordinalPosition,
            boolean nullable,
            String remarks,
            String parentExternalId,
            String externalId
    ) {
        Map<String, String> attributes = baseAttributes(
                externalId, parentExternalId, qualifiedName(database, table, column));
        attributes.put(DatasourceObjectAttributes.DATABASE, database);
        attributes.put(DatasourceObjectAttributes.TABLE, table);
        attributes.put(DatasourceObjectAttributes.ORDINAL_POSITION,
                Integer.toString(ordinalPosition));
        attributes.put(DatasourceObjectAttributes.DATA_TYPE,
                textOrDefault(nativeType, "unknown"));
        attributes.put(DatasourceObjectAttributes.NATIVE_TYPE,
                textOrDefault(nativeType, "unknown"));
        attributes.put(DatasourceObjectAttributes.NULLABLE, Boolean.toString(nullable));
        putOptional(attributes, DatasourceObjectAttributes.REMARKS, remarks);
        return new DatasourceObject(column, DatasourceObjectKinds.COLUMN, attributes);
    }

    private static Map<String, String> baseAttributes(
            String externalId,
            String parentExternalId,
            String qualifiedName
    ) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put(DatasourceObjectAttributes.EXTERNAL_ID, externalId);
        attributes.put(DatasourceObjectAttributes.PARENT_EXTERNAL_ID, parentExternalId);
        attributes.put(DatasourceObjectAttributes.QUALIFIED_NAME, qualifiedName);
        return attributes;
    }

    private static void putOptional(
            Map<String, String> attributes,
            String key,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value.trim());
        }
    }

    private static String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static boolean includeViews(Map<String, String> options) {
        String value = options.getOrDefault("includeViews", "true").trim().toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("includeViews must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static String externalId(String type, String... parts) {
        StringBuilder value = new StringBuilder("mysql.").append(type);
        for (String part : parts) {
            value.append('/').append(encode(part));
        }
        return value.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(
                Objects.requireNonNull(value, "identifier must not be null"),
                StandardCharsets.UTF_8
        ).replace("+", "%20");
    }

    private static String qualifiedName(String... parts) {
        return String.join(".", parts);
    }

    private static void bind(
            PreparedStatement statement,
            List<IntegrationValue> parameters
    ) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            int parameterIndex = index + 1;
            IntegrationValue value = parameters.get(index);
            switch (value) {
                case IntegrationValue.NullValue ignored -> statement.setObject(parameterIndex, null);
                case IntegrationValue.TextValue text ->
                        statement.setString(parameterIndex, text.value());
                case IntegrationValue.BooleanValue bool ->
                        statement.setBoolean(parameterIndex, bool.value());
                case IntegrationValue.DecimalValue decimal ->
                        statement.setBigDecimal(parameterIndex, decimal.value());
                case IntegrationValue.BinaryValue binary ->
                        statement.setBytes(parameterIndex, binary.bytes());
                case IntegrationValue.InstantValue instant ->
                        statement.setTimestamp(parameterIndex, Timestamp.from(instant.value()));
                case IntegrationValue.DateValue date ->
                        statement.setDate(parameterIndex, java.sql.Date.valueOf(date.value()));
                case IntegrationValue.DateTimeValue dateTime ->
                        statement.setTimestamp(parameterIndex, Timestamp.valueOf(dateTime.value()));
                case IntegrationValue.ArrayValue ignored -> throw invalidParameter("array");
                case IntegrationValue.ObjectValue ignored -> throw invalidParameter("object");
            }
        }
    }

    private static ExternalSystemException invalidParameter(String type) {
        return new ExternalSystemException(
                IntegrationErrorCode.INVALID_REQUEST,
                "MySQL adapter does not support " + type + " query parameters",
                false
        );
    }

    private static QueryResult readQueryResult(ResultSet resultSet, int maxRows)
            throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<String> columns = uniqueColumnLabels(metadata);
        List<List<IntegrationValue>> rows = new ArrayList<>();
        boolean truncated = false;
        while (resultSet.next()) {
            if (rows.size() == maxRows) {
                truncated = true;
                break;
            }
            List<IntegrationValue> row = new ArrayList<>(columns.size());
            for (int columnIndex = 1; columnIndex <= columns.size(); columnIndex++) {
                row.add(toIntegrationValue(resultSet.getObject(columnIndex)));
            }
            rows.add(List.copyOf(row));
        }
        return new QueryResult(columns, rows, truncated);
    }

    private static List<String> uniqueColumnLabels(ResultSetMetaData metadata) throws SQLException {
        List<String> labels = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        for (int columnIndex = 1; columnIndex <= metadata.getColumnCount(); columnIndex++) {
            String base = textOrDefault(metadata.getColumnLabel(columnIndex),
                    "column_" + columnIndex);
            int occurrence = occurrences.merge(base, 1, Integer::sum);
            labels.add(occurrence == 1 ? base : base + "_" + occurrence);
        }
        return List.copyOf(labels);
    }

    private static IntegrationValue toIntegrationValue(Object value) {
        if (value == null) {
            return new IntegrationValue.NullValue();
        }
        if (value instanceof String text) {
            return new IntegrationValue.TextValue(text);
        }
        if (value instanceof Character character) {
            return new IntegrationValue.TextValue(character.toString());
        }
        if (value instanceof Boolean bool) {
            return new IntegrationValue.BooleanValue(bool);
        }
        if (value instanceof BigDecimal decimal) {
            return new IntegrationValue.DecimalValue(decimal);
        }
        if (value instanceof Number number) {
            return new IntegrationValue.DecimalValue(new BigDecimal(number.toString()));
        }
        if (value instanceof byte[] binary) {
            return IntegrationValue.BinaryValue.fromBytes(binary);
        }
        if (value instanceof java.sql.Date date) {
            return new IntegrationValue.DateValue(date.toLocalDate());
        }
        if (value instanceof Timestamp timestamp) {
            return new IntegrationValue.InstantValue(timestamp.toInstant());
        }
        if (value instanceof Instant instant) {
            return new IntegrationValue.InstantValue(instant);
        }
        if (value instanceof LocalDate date) {
            return new IntegrationValue.DateValue(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return new IntegrationValue.DateTimeValue(dateTime);
        }
        return new IntegrationValue.TextValue(value.toString());
    }

    private void checkDeadline(AdapterRequestContext context, String operation) {
        if (context.isExpired(clock)) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.TIMEOUT,
                    "MySQL " + operation + " deadline has expired",
                    true,
                    Map.of("operation", operation),
                    null
            );
        }
    }

    private int validationTimeoutSeconds(AdapterRequestContext context) {
        long remaining = Duration.between(clock.instant(), context.deadline()).toSeconds();
        return (int) Math.max(1, Math.min(30, remaining));
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - startedAt));
    }

    private static ExternalSystemException externalFailure(
            String operation,
            SQLException exception
    ) {
        IntegrationErrorCode errorCode = errorCode(exception.getSQLState());
        return new ExternalSystemException(
                errorCode,
                "MySQL " + operation + " failed",
                retryable(exception.getSQLState()),
                errorDetails(exception, operation),
                exception
        );
    }

    private static Map<String, String> errorDetails(
            SQLException exception,
            String operation
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("adapterId", ADAPTER_ID);
        details.put("operation", operation);
        details.put("vendorCode", Integer.toString(exception.getErrorCode()));
        if (exception.getSQLState() != null && !exception.getSQLState().isBlank()) {
            details.put("sqlState", exception.getSQLState());
        }
        return Map.copyOf(details);
    }

    private static IntegrationErrorCode errorCode(String sqlState) {
        if (sqlState == null) {
            return IntegrationErrorCode.EXTERNAL_FAILURE;
        }
        if (sqlState.startsWith("28")) {
            return IntegrationErrorCode.AUTHENTICATION_FAILED;
        }
        if (sqlState.startsWith("42")) {
            return IntegrationErrorCode.INVALID_REQUEST;
        }
        if (sqlState.startsWith("23")) {
            return IntegrationErrorCode.CONFLICT;
        }
        if (sqlState.startsWith("08")) {
            return IntegrationErrorCode.UNAVAILABLE;
        }
        if (sqlState.startsWith("HYT")) {
            return IntegrationErrorCode.TIMEOUT;
        }
        return IntegrationErrorCode.EXTERNAL_FAILURE;
    }

    private static boolean retryable(String sqlState) {
        return sqlState != null && (sqlState.startsWith("08")
                || sqlState.startsWith("40")
                || sqlState.startsWith("HYT"));
    }
}
