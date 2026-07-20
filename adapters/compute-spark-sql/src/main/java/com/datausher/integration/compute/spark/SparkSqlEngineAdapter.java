package com.datausher.integration.compute.spark;

import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.ComputeJobHandle;
import com.datausher.integration.compute.api.ComputeJobLogEntry;
import com.datausher.integration.compute.api.ComputeJobLogPage;
import com.datausher.integration.compute.api.ComputeJobRequest;
import com.datausher.integration.compute.api.ComputeJobResultPage;
import com.datausher.integration.compute.api.ComputeJobState;
import com.datausher.integration.compute.api.ComputeJobStatus;
import com.datausher.integration.compute.api.ComputeResultColumn;
import com.datausher.integration.compute.api.SqlEngineAdapter;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.compute.api.SqlExplainPlan;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SparkSqlEngineAdapter implements SqlEngineAdapter {
    private static final int DEFAULT_MAX_ROWS = 1_000;
    private static final int MAX_ALLOWED_ROWS = 100_000;

    private final AdapterDescriptor descriptor;
    private final SparkSqlConnectionFactory connectionFactory;
    private final Clock clock;
    private final ConcurrentMap<String, SparkJob> jobs = new ConcurrentHashMap<>();

    public SparkSqlEngineAdapter(SparkSqlConnectionFactory connectionFactory) {
        this("spark-sql", connectionFactory, Clock.systemUTC());
    }

    public SparkSqlEngineAdapter(
            String adapterId,
            SparkSqlConnectionFactory connectionFactory,
            Clock clock
    ) {
        this.descriptor = new AdapterDescriptor(
                adapterId,
                AdapterType.COMPUTE_ENGINE,
                "1.0.0",
                Set.of(
                        AdapterCapability.of(ComputeCapabilities.JOB_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.JOB_LOGS),
                        AdapterCapability.of(ComputeCapabilities.JOB_RESULTS),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXPLAIN)
                ),
                Map.of("engine", "spark-sql", "protocol", "jdbc")
        );
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AdapterDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public AdapterHealth checkHealth() {
        return new AdapterHealth(
                descriptor.adapterId(),
                AdapterHealthStatus.UP,
                clock.instant(),
                "adapter ready",
                Map.of("credentialHandling", "external")
        );
    }

    @Override
    public ComputeJobHandle submit(AdapterRequestContext context, ComputeJobRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        checkDeadline(context, "submit");
        if (!"sql".equals(request.workloadType())) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.INVALID_REQUEST,
                    "Spark SQL adapter only supports SQL workloads",
                    false,
                    Map.of("workloadType", request.workloadType()),
                    null
            );
        }
        int maxRows = maxRows(request.options());
        ComputeJobHandle handle = new ComputeJobHandle(
                descriptor.adapterId(), request.bindingId(), "spark-" + UUID.randomUUID());
        Instant startedAt = clock.instant();
        SparkJob job = execute(handle, request, maxRows, startedAt);
        jobs.put(handle.externalJobId(), job);
        return handle;
    }

    @Override
    public ComputeJobStatus status(AdapterRequestContext context, ComputeJobHandle handle) {
        Objects.requireNonNull(context, "context must not be null");
        SparkJob job = requireJob(handle);
        return new ComputeJobStatus(
                job.handle(), job.state(), clock.instant(), job.message(), job.details());
    }

    @Override
    public void cancel(AdapterRequestContext context, ComputeJobHandle handle) {
        Objects.requireNonNull(context, "context must not be null");
        SparkJob job = requireJob(handle);
        if (job.state() == ComputeJobState.QUEUED || job.state() == ComputeJobState.RUNNING) {
            jobs.put(handle.externalJobId(), job.cancelled(clock.instant()));
        }
    }

    @Override
    public ComputeJobLogPage readLogs(
            AdapterRequestContext context,
            ComputeJobHandle handle,
            long afterSequence,
            int limit
    ) {
        Objects.requireNonNull(context, "context must not be null");
        if (afterSequence < -1 || limit < 1) {
            throw new IllegalArgumentException("afterSequence must be at least -1 and limit must be positive");
        }
        SparkJob job = requireJob(handle);
        List<ComputeJobLogEntry> entries = job.logs().stream()
                .filter(entry -> entry.sequence() > afterSequence)
                .limit(limit)
                .toList();
        long nextSequence = entries.isEmpty() ? afterSequence
                : entries.getLast().sequence() + 1;
        boolean complete = entries.size() < limit;
        return new ComputeJobLogPage(handle, entries, nextSequence, complete);
    }

    @Override
    public ComputeJobResultPage readResult(
            AdapterRequestContext context,
            ComputeJobHandle handle,
            long offset,
            int limit
    ) {
        Objects.requireNonNull(context, "context must not be null");
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("offset must be non-negative and limit must be positive");
        }
        SparkJob job = requireJob(handle);
        if (job.state() != ComputeJobState.SUCCEEDED) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.CONFLICT,
                    "Spark SQL job results are available only after success",
                    false,
                    Map.of("state", job.state().name()),
                    null
            );
        }
        int fromIndex = (int) Math.min(offset, job.rows().size());
        int toIndex = (int) Math.min(job.rows().size(), offset + limit);
        return new ComputeJobResultPage(
                handle,
                job.columns(),
                job.rows().subList(fromIndex, toIndex),
                offset,
                job.affectedRows(),
                toIndex < job.rows().size(),
                job.resultReference(),
                Map.of("engine", "spark-sql")
        );
    }

    @Override
    public SqlExplainPlan explain(AdapterRequestContext context, SqlExecutionRequest request) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        checkDeadline(context, "explain");
        ComputeJobRequest jobRequest = new ComputeJobRequest(
                request.bindingId(), "sql", "EXPLAIN " + request.statement(),
                request.parameters(), request.options());
        try (Connection connection = connectionFactory.open(jobRequest);
             PreparedStatement statement = connection.prepareStatement(jobRequest.payload())) {
            bind(statement, request.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                StringBuilder content = new StringBuilder();
                while (resultSet.next()) {
                    if (!content.isEmpty()) {
                        content.append(System.lineSeparator());
                    }
                    content.append(resultSet.getString(1));
                }
                return new SqlExplainPlan(
                        "text",
                        content.isEmpty() ? "No explain output returned" : content.toString(),
                        Map.of("engine", "spark-sql")
                );
            }
        } catch (SQLException exception) {
            throw mapSqlException("explain", exception);
        }
    }

    private SparkJob execute(
            ComputeJobHandle handle,
            ComputeJobRequest request,
            int maxRows,
            Instant startedAt
    ) {
        List<ComputeJobLogEntry> logs = new ArrayList<>();
        logs.add(new ComputeJobLogEntry(
                0, startedAt, "INFO", "Spark SQL job accepted", Map.of("engine", "spark-sql")));
        try (Connection connection = connectionFactory.open(request);
             PreparedStatement statement = connection.prepareStatement(request.payload())) {
            bind(statement, request.parameters());
            boolean hasResultSet = statement.execute();
            List<ComputeResultColumn> columns = List.of();
            List<List<IntegrationValue>> rows = List.of();
            long affectedRows = 0;
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    ResultSetData data = readRows(resultSet, maxRows);
                    columns = data.columns();
                    rows = data.rows();
                }
            } else {
                affectedRows = Math.max(statement.getUpdateCount(), 0);
            }
            logs.add(new ComputeJobLogEntry(
                    1, clock.instant(), "INFO", "Spark SQL job completed", Map.of()));
            return new SparkJob(
                    handle, ComputeJobState.SUCCEEDED, "Spark SQL job succeeded", Map.of(),
                    columns, rows, affectedRows, "", List.copyOf(logs));
        } catch (SQLException exception) {
            logs.add(new ComputeJobLogEntry(
                    1, clock.instant(), "ERROR", "Spark SQL job failed",
                    Map.of("sqlState", safe(exception.getSQLState()))));
            return new SparkJob(
                    handle, ComputeJobState.FAILED, "Spark SQL job failed",
                    Map.of("sqlState", safe(exception.getSQLState())),
                    List.of(), List.of(), 0, "", List.copyOf(logs));
        }
    }

    private SparkJob requireJob(ComputeJobHandle handle) {
        Objects.requireNonNull(handle, "handle must not be null");
        if (!descriptor.adapterId().equals(handle.adapterId())) {
            throw new IllegalArgumentException("handle adapterId does not match this adapter");
        }
        SparkJob job = jobs.get(handle.externalJobId());
        if (job == null) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.NOT_FOUND,
                    "Spark SQL job does not exist",
                    false,
                    Map.of("externalJobId", handle.externalJobId()),
                    null
            );
        }
        return job;
    }

    private static ResultSetData readRows(ResultSet resultSet, int maxRows) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        List<ComputeResultColumn> columns = new ArrayList<>();
        for (int index = 1; index <= columnCount; index++) {
            String name = metadata.getColumnLabel(index);
            if (name == null || name.isBlank()) {
                name = metadata.getColumnName(index);
            }
            columns.add(new ComputeResultColumn(
                    name, metadata.getColumnTypeName(index), metadata.isNullable(index) != ResultSetMetaData.columnNoNulls,
                    Map.of("jdbcType", Integer.toString(metadata.getColumnType(index)))));
        }
        List<List<IntegrationValue>> rows = new ArrayList<>();
        while (resultSet.next() && rows.size() < maxRows) {
            List<IntegrationValue> row = new ArrayList<>();
            for (int index = 1; index <= columnCount; index++) {
                row.add(toValue(resultSet.getObject(index)));
            }
            rows.add(List.copyOf(row));
        }
        return new ResultSetData(List.copyOf(columns), List.copyOf(rows));
    }

    private static void bind(
            PreparedStatement statement,
            Map<String, IntegrationValue> parameters
    ) throws SQLException {
        List<Map.Entry<String, IntegrationValue>> entries = parameters.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> parameterIndex(entry.getKey())))
                .toList();
        for (Map.Entry<String, IntegrationValue> entry : entries) {
            bind(statement, parameterIndex(entry.getKey()), entry.getValue());
        }
    }

    private static int parameterIndex(String key) {
        try {
            int index = Integer.parseInt(key);
            if (index < 1) {
                throw new IllegalArgumentException("parameter index must be positive: " + key);
            }
            return index;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("parameter name must be a positive JDBC index: " + key, exception);
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

    private static int maxRows(Map<String, String> options) {
        String value = options.getOrDefault("maxRows", Integer.toString(DEFAULT_MAX_ROWS));
        try {
            int rows = Integer.parseInt(value);
            if (rows < 1 || rows > MAX_ALLOWED_ROWS) {
                throw new IllegalArgumentException(
                        "maxRows must be between 1 and " + MAX_ALLOWED_ROWS);
            }
            return rows;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("maxRows must be an integer", exception);
        }
    }

    private void checkDeadline(AdapterRequestContext context, String operation) {
        if (context.isExpired(clock)) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.TIMEOUT,
                    "Spark SQL " + operation + " request expired before adapter execution",
                    true,
                    Map.of("adapterId", descriptor.adapterId()),
                    null
            );
        }
    }

    private static ExternalSystemException mapSqlException(String operation, SQLException exception) {
        return new ExternalSystemException(
                IntegrationErrorCode.EXTERNAL_FAILURE,
                "Spark SQL " + operation + " failed",
                false,
                Map.of("sqlState", safe(exception.getSQLState())),
                exception
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private record ResultSetData(
            List<ComputeResultColumn> columns,
            List<List<IntegrationValue>> rows
    ) {
    }

    private record SparkJob(
            ComputeJobHandle handle,
            ComputeJobState state,
            String message,
            Map<String, String> details,
            List<ComputeResultColumn> columns,
            List<List<IntegrationValue>> rows,
            long affectedRows,
            String resultReference,
            List<ComputeJobLogEntry> logs
    ) {
        SparkJob {
            details = details == null ? Map.of() : Map.copyOf(details);
            columns = columns == null ? List.of() : List.copyOf(columns);
            rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
            logs = logs == null ? List.of() : List.copyOf(logs);
        }

        SparkJob cancelled(Instant cancelledAt) {
            List<ComputeJobLogEntry> updatedLogs = new ArrayList<>(logs);
            updatedLogs.add(new ComputeJobLogEntry(
                    logs.size(), cancelledAt, "WARN", "Spark SQL job cancelled", Map.of()));
            return new SparkJob(
                    handle, ComputeJobState.CANCELLED, "Spark SQL job cancelled", details,
                    columns, rows, affectedRows, resultReference, List.copyOf(updatedLogs));
        }
    }
}
