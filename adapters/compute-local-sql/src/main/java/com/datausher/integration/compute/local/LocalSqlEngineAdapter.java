package com.datausher.integration.compute.local;

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
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalSqlEngineAdapter implements SqlEngineAdapter, AutoCloseable {
    private static final int DEFAULT_MAX_ROWS = 1_000;
    private static final int MAX_ALLOWED_ROWS = 100_000;

    private final AdapterDescriptor descriptor;
    private final LocalSqlConnectionFactory connectionFactory;
    private final ExecutorService executor;
    private final Clock clock;
    private final boolean ownsExecutor;
    private final ConcurrentMap<String, LocalJob> jobs = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public LocalSqlEngineAdapter(
            String adapterId,
            LocalSqlConnectionFactory connectionFactory
    ) {
        this(adapterId, connectionFactory, Executors.newVirtualThreadPerTaskExecutor(),
                Clock.systemUTC(), true);
    }

    public LocalSqlEngineAdapter(
            String adapterId,
            LocalSqlConnectionFactory connectionFactory,
            ExecutorService executor,
            Clock clock
    ) {
        this(adapterId, connectionFactory, executor, clock, false);
    }

    private LocalSqlEngineAdapter(
            String adapterId,
            LocalSqlConnectionFactory connectionFactory,
            ExecutorService executor,
            Clock clock,
            boolean ownsExecutor
    ) {
        this.descriptor = new AdapterDescriptor(
                adapterId,
                AdapterType.COMPUTE_ENGINE,
                "1.0.0",
                Set.of(
                        AdapterCapability.of(ComputeCapabilities.JOB_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.JOB_CANCELLATION),
                        AdapterCapability.of(ComputeCapabilities.JOB_LOGS),
                        AdapterCapability.of(ComputeCapabilities.JOB_RESULTS),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXECUTION),
                        AdapterCapability.of(ComputeCapabilities.SQL_EXPLAIN)
                ),
                Map.of("engine", "local-jdbc")
        );
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "connectionFactory must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.ownsExecutor = ownsExecutor;
    }

    @Override
    public ComputeJobHandle submit(
            AdapterRequestContext context,
            ComputeJobRequest request
    ) {
        requireOpen();
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        if (!"sql".equals(request.workloadType())) {
            throw new IllegalArgumentException(
                    "local SQL adapter supports only the sql workload type");
        }
        String externalJobId = UUID.randomUUID().toString();
        ComputeJobHandle handle = new ComputeJobHandle(
                descriptor.adapterId(), request.bindingId(), externalJobId);
        LocalJob job = new LocalJob(handle, clock.instant());
        if (jobs.putIfAbsent(externalJobId, job) != null) {
            throw new IllegalStateException("duplicate local job ID");
        }
        Future<?> future = executor.submit(() -> execute(job, request));
        job.attachFuture(future);
        return handle;
    }

    @Override
    public ComputeJobStatus status(
            AdapterRequestContext context,
            ComputeJobHandle handle
    ) {
        Objects.requireNonNull(context, "context must not be null");
        return requireJob(handle).status(clock.instant());
    }

    @Override
    public void cancel(AdapterRequestContext context, ComputeJobHandle handle) {
        Objects.requireNonNull(context, "context must not be null");
        requireJob(handle).cancel(clock.instant());
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
            throw new IllegalArgumentException(
                    "afterSequence must be at least -1 and limit must be positive");
        }
        return requireJob(handle).logs(afterSequence, limit);
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
            throw new IllegalArgumentException(
                    "offset must not be negative and limit must be positive");
        }
        return requireJob(handle).result(offset, limit);
    }

    @Override
    public SqlExplainPlan explain(
            AdapterRequestContext context,
            SqlExecutionRequest request
    ) {
        requireOpen();
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(request, "request must not be null");
        try (Connection connection = connectionFactory.open(request.bindingId());
             PreparedStatement statement = connection.prepareStatement(
                     "EXPLAIN " + request.statement())) {
            bindParameters(statement, request.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ExternalSystemException(
                            IntegrationErrorCode.EXTERNAL_FAILURE,
                            "local SQL engine returned no explain plan",
                            false);
                }
                return new SqlExplainPlan(
                        "text", resultSet.getString(1), Map.of("engine", "local-jdbc"));
            }
        } catch (SQLException failure) {
            throw mapFailure(failure, "local SQL explain failed");
        }
    }

    @Override
    public AdapterDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public AdapterHealth checkHealth() {
        Instant checkedAt = clock.instant();
        if (closed.get()) {
            return new AdapterHealth(
                    descriptor.adapterId(), AdapterHealthStatus.DOWN,
                    checkedAt, "adapter is closed", Map.of());
        }
        try (Connection connection = connectionFactory.open("health-check")) {
            boolean valid = connection.isValid(2);
            return new AdapterHealth(
                    descriptor.adapterId(),
                    valid ? AdapterHealthStatus.UP : AdapterHealthStatus.DEGRADED,
                    checkedAt,
                    valid ? "ready" : "connection validation failed",
                    Map.of("engine", "local-jdbc")
            );
        } catch (SQLException failure) {
            return new AdapterHealth(
                    descriptor.adapterId(), AdapterHealthStatus.DOWN, checkedAt,
                    "connection failed", safeSqlDetails(failure));
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            jobs.values().forEach(job -> job.cancel(clock.instant()));
            if (ownsExecutor) {
                executor.close();
            }
        }
    }

    private void execute(LocalJob job, ComputeJobRequest request) {
        if (!job.start(clock.instant())) {
            return;
        }
        try (Connection connection = connectionFactory.open(request.bindingId());
             PreparedStatement statement = connection.prepareStatement(request.payload())) {
            job.attachStatement(statement);
            int maxRows = maxRows(request.options());
            statement.setMaxRows(maxRows + 1);
            bindParameters(statement, request.parameters());
            boolean resultSetAvailable = statement.execute();
            if (resultSetAvailable) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    job.succeed(readResult(resultSet, maxRows), clock.instant());
                }
            } else {
                job.succeed(LocalResult.updateCount(statement.getUpdateCount()), clock.instant());
            }
        } catch (SQLException failure) {
            if (job.cancelRequested()) {
                job.cancel(clock.instant());
            } else {
                job.fail("local SQL execution failed", safeSqlDetails(failure), clock.instant());
            }
        } catch (RuntimeException failure) {
            if (job.cancelRequested()) {
                job.cancel(clock.instant());
            } else {
                job.fail("local SQL execution failed", Map.of(), clock.instant());
            }
        } finally {
            job.detachStatement();
        }
    }

    private static LocalResult readResult(ResultSet resultSet, int maxRows) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<ComputeResultColumn> columns = new ArrayList<>(metadata.getColumnCount());
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            columns.add(new ComputeResultColumn(
                    metadata.getColumnLabel(index),
                    metadata.getColumnTypeName(index),
                    metadata.isNullable(index) != ResultSetMetaData.columnNoNulls,
                    Map.of("jdbcType", Integer.toString(metadata.getColumnType(index)))
            ));
        }
        List<List<IntegrationValue>> rows = new ArrayList<>();
        boolean truncated = false;
        while (resultSet.next()) {
            if (rows.size() == maxRows) {
                truncated = true;
                break;
            }
            List<IntegrationValue> row = new ArrayList<>(columns.size());
            for (int index = 1; index <= columns.size(); index++) {
                row.add(toIntegrationValue(resultSet.getObject(index)));
            }
            rows.add(List.copyOf(row));
        }
        return new LocalResult(columns, rows, 0, truncated);
    }

    private static void bindParameters(
            PreparedStatement statement,
            Map<String, IntegrationValue> parameters
    ) throws SQLException {
        List<Map.Entry<String, IntegrationValue>> ordered = parameters.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> parameterIndex(entry.getKey())))
                .toList();
        for (Map.Entry<String, IntegrationValue> entry : ordered) {
            int index = parameterIndex(entry.getKey());
            statement.setObject(index, toJdbcValue(entry.getValue()));
        }
    }

    private static int parameterIndex(String name) {
        try {
            int index = Integer.parseInt(name);
            if (index < 1) {
                throw new IllegalArgumentException("SQL parameter indexes must start at 1");
            }
            return index;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "local SQL parameter names must be positive JDBC indexes", failure);
        }
    }

    private static Object toJdbcValue(IntegrationValue value) {
        return switch (value) {
            case IntegrationValue.NullValue ignored -> null;
            case IntegrationValue.TextValue text -> text.value();
            case IntegrationValue.BooleanValue bool -> bool.value();
            case IntegrationValue.DecimalValue decimal -> decimal.value();
            case IntegrationValue.BinaryValue binary -> binary.bytes();
            case IntegrationValue.InstantValue instant -> Timestamp.from(instant.value());
            case IntegrationValue.DateValue date -> Date.valueOf(date.value());
            case IntegrationValue.DateTimeValue dateTime ->
                    Timestamp.valueOf(dateTime.value());
            case IntegrationValue.ArrayValue ignored -> throw new IllegalArgumentException(
                    "local SQL does not support array parameters");
            case IntegrationValue.ObjectValue ignored -> throw new IllegalArgumentException(
                    "local SQL does not support object parameters");
        };
    }

    private static IntegrationValue toIntegrationValue(Object value) {
        if (value == null) {
            return new IntegrationValue.NullValue();
        }
        if (value instanceof Boolean bool) {
            return new IntegrationValue.BooleanValue(bool);
        }
        if (value instanceof BigDecimal decimal) {
            return new IntegrationValue.DecimalValue(decimal);
        }
        if (value instanceof Number number) {
            return new IntegrationValue.DecimalValue(number.toString().contains(".")
                    ? new BigDecimal(number.toString())
                    : BigDecimal.valueOf(number.longValue()));
        }
        if (value instanceof byte[] bytes) {
            return new IntegrationValue.BinaryValue(
                    Base64.getEncoder().encodeToString(bytes));
        }
        if (value instanceof Timestamp timestamp) {
            return new IntegrationValue.InstantValue(timestamp.toInstant());
        }
        if (value instanceof Date date) {
            return new IntegrationValue.DateValue(date.toLocalDate());
        }
        if (value instanceof LocalDate date) {
            return new IntegrationValue.DateValue(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return new IntegrationValue.DateTimeValue(dateTime);
        }
        if (value instanceof OffsetDateTime dateTime) {
            return new IntegrationValue.InstantValue(dateTime.toInstant());
        }
        return new IntegrationValue.TextValue(value.toString());
    }

    private static int maxRows(Map<String, String> options) {
        String value = options.get("maxRows");
        if (value == null) {
            return DEFAULT_MAX_ROWS;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > MAX_ALLOWED_ROWS) {
                throw new IllegalArgumentException(
                        "maxRows must be between 1 and " + MAX_ALLOWED_ROWS);
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("maxRows must be an integer", failure);
        }
    }

    private LocalJob requireJob(ComputeJobHandle handle) {
        validateHandle(handle);
        LocalJob job = Optional.ofNullable(jobs.get(handle.externalJobId())).orElseThrow(() ->
                new IllegalArgumentException("local SQL job does not exist"));
        if (!job.handle.equals(handle)) {
            throw new IllegalArgumentException("job handle ownership does not match");
        }
        return job;
    }

    private void validateHandle(ComputeJobHandle handle) {
        Objects.requireNonNull(handle, "handle must not be null");
        if (!descriptor.adapterId().equals(handle.adapterId())) {
            throw new IllegalArgumentException("job handle belongs to a different adapter");
        }
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new IllegalStateException("local SQL adapter is closed");
        }
    }

    private static ExternalSystemException mapFailure(SQLException failure, String message) {
        return new ExternalSystemException(
                IntegrationErrorCode.EXTERNAL_FAILURE,
                message,
                false,
                safeSqlDetails(failure),
                failure
        );
    }

    private static Map<String, String> safeSqlDetails(SQLException failure) {
        Map<String, String> details = new LinkedHashMap<>();
        if (failure.getSQLState() != null && !failure.getSQLState().isBlank()) {
            details.put("sqlState", failure.getSQLState());
        }
        details.put("vendorCode", Integer.toString(failure.getErrorCode()));
        return Map.copyOf(details);
    }

    private static final class LocalJob {
        private final ComputeJobHandle handle;
        private final List<ComputeJobLogEntry> logs = new ArrayList<>();
        private ComputeJobState state = ComputeJobState.QUEUED;
        private String message = "queued";
        private Map<String, String> details = Map.of();
        private Future<?> future;
        private Statement statement;
        private LocalResult result;
        private boolean cancelRequested;

        private LocalJob(ComputeJobHandle handle, Instant createdAt) {
            this.handle = handle;
            appendLog(createdAt, "INFO", "queued");
        }

        synchronized void attachFuture(Future<?> nextFuture) {
            future = nextFuture;
            if (cancelRequested) {
                future.cancel(true);
            }
        }

        synchronized boolean start(Instant startedAt) {
            if (cancelRequested || state == ComputeJobState.CANCELLED) {
                return false;
            }
            state = ComputeJobState.RUNNING;
            message = "running";
            appendLog(startedAt, "INFO", "running");
            return true;
        }

        synchronized void attachStatement(Statement nextStatement) throws SQLException {
            statement = nextStatement;
            if (cancelRequested) {
                statement.cancel();
            }
        }

        synchronized void detachStatement() {
            statement = null;
        }

        synchronized void succeed(LocalResult nextResult, Instant completedAt) {
            if (cancelRequested || state == ComputeJobState.CANCELLED) {
                return;
            }
            result = nextResult;
            state = ComputeJobState.SUCCEEDED;
            message = "succeeded";
            appendLog(completedAt, "INFO", "succeeded");
        }

        synchronized void fail(
                String failureMessage,
                Map<String, String> failureDetails,
                Instant failedAt
        ) {
            if (cancelRequested || state == ComputeJobState.CANCELLED) {
                return;
            }
            state = ComputeJobState.FAILED;
            message = failureMessage;
            details = Map.copyOf(failureDetails);
            appendLog(failedAt, "ERROR", failureMessage);
        }

        synchronized void cancel(Instant cancelledAt) {
            if (state == ComputeJobState.SUCCEEDED || state == ComputeJobState.FAILED
                    || state == ComputeJobState.CANCELLED) {
                return;
            }
            cancelRequested = true;
            if (statement != null) {
                try {
                    statement.cancel();
                } catch (SQLException ignored) {
                    details = Map.of("cancel", "statement cancellation failed");
                }
            }
            if (future != null) {
                future.cancel(true);
            }
            state = ComputeJobState.CANCELLED;
            message = "cancelled";
            appendLog(cancelledAt, "INFO", "cancelled");
        }

        synchronized boolean cancelRequested() {
            return cancelRequested;
        }

        synchronized ComputeJobStatus status(Instant observedAt) {
            return new ComputeJobStatus(handle, state, observedAt, message, details);
        }

        synchronized ComputeJobLogPage logs(long afterSequence, int limit) {
            List<ComputeJobLogEntry> page = logs.stream()
                    .filter(entry -> entry.sequence() > afterSequence)
                    .limit(limit)
                    .toList();
            long nextSequence = page.isEmpty()
                    ? Math.max(afterSequence + 1, 0)
                    : page.getLast().sequence() + 1;
            boolean complete = terminal()
                    && (logs.isEmpty() || nextSequence > logs.getLast().sequence());
            return new ComputeJobLogPage(handle, page, nextSequence, complete);
        }

        synchronized ComputeJobResultPage result(long offset, int limit) {
            if (state != ComputeJobState.SUCCEEDED || result == null) {
                throw new IllegalStateException("local SQL result is not available");
            }
            int fromIndex = (int) Math.min(offset, result.rows().size());
            int toIndex = Math.min(fromIndex + limit, result.rows().size());
            Map<String, String> attributes = result.truncated()
                    ? Map.of("truncated", "true")
                    : Map.of();
            return new ComputeJobResultPage(
                    handle,
                    result.columns(),
                    result.rows().subList(fromIndex, toIndex),
                    offset,
                    result.affectedRows(),
                    toIndex < result.rows().size(),
                    "local-sql://" + handle.externalJobId(),
                    attributes
            );
        }

        private boolean terminal() {
            return state == ComputeJobState.SUCCEEDED
                    || state == ComputeJobState.FAILED
                    || state == ComputeJobState.CANCELLED;
        }

        private void appendLog(Instant occurredAt, String level, String message) {
            logs.add(new ComputeJobLogEntry(
                    logs.size(), occurredAt, level, message, Map.of()));
        }
    }

    private record LocalResult(
            List<ComputeResultColumn> columns,
            List<List<IntegrationValue>> rows,
            long affectedRows,
            boolean truncated
    ) {
        private LocalResult {
            columns = List.copyOf(columns);
            rows = rows.stream().map(List::copyOf).toList();
        }

        static LocalResult updateCount(long affectedRows) {
            return new LocalResult(List.of(), List.of(), Math.max(affectedRows, 0), false);
        }
    }
}
