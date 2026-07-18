package com.datausher.execution.sql.core;

import com.datausher.execution.api.ExecutionAccount;
import com.datausher.execution.api.ExecutionAccountQueryService;
import com.datausher.execution.api.ExecutionAccountStatus;
import com.datausher.execution.sql.api.SqlExplainPlan;
import com.datausher.execution.sql.api.SqlExplainRequest;
import com.datausher.execution.sql.api.SqlExplainService;
import com.datausher.execution.sql.api.SqlWorkloads;
import com.datausher.integration.compute.api.ComputeCapabilities;
import com.datausher.integration.compute.api.SqlEngineAdapter;
import com.datausher.integration.compute.api.SqlExecutionRequest;
import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRegistry;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.platform.shared.time.Clock;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DefaultSqlExplainService implements SqlExplainService {
    private final ExecutionAccountQueryService accountQueryService;
    private final AdapterRegistry adapterRegistry;
    private final AdapterInvocationExecutor invocationExecutor;
    private final Clock clock;
    private final Duration adapterTimeout;

    public DefaultSqlExplainService(
            ExecutionAccountQueryService accountQueryService,
            AdapterRegistry adapterRegistry,
            AdapterInvocationExecutor invocationExecutor,
            Clock clock,
            Duration adapterTimeout
    ) {
        this.accountQueryService = Objects.requireNonNull(
                accountQueryService, "accountQueryService must not be null");
        this.adapterRegistry = Objects.requireNonNull(
                adapterRegistry, "adapterRegistry must not be null");
        this.invocationExecutor = Objects.requireNonNull(
                invocationExecutor, "invocationExecutor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.adapterTimeout = Objects.requireNonNull(
                adapterTimeout, "adapterTimeout must not be null");
        if (adapterTimeout.isZero() || adapterTimeout.isNegative()) {
            throw new IllegalArgumentException("adapterTimeout must be positive");
        }
    }

    @Override
    public SqlExplainPlan explain(SqlExplainRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ExecutionAccount account = accountQueryService.findAccount(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "execution account does not exist: " + request.accountId()));
        if (account.status() != ExecutionAccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "execution account is not active: " + request.accountId());
        }
        if (!account.supports(SqlWorkloads.TYPE)) {
            throw new IllegalArgumentException(
                    "execution account does not support SQL workloads: " + request.accountId());
        }
        SqlEngineAdapter adapter = adapterRegistry.find(
                account.adapterId(), SqlEngineAdapter.class).orElseThrow(() ->
                new IllegalStateException(
                        "SQL compute adapter is not registered: " + account.adapterId()));
        if (!adapter.descriptor().supports(ComputeCapabilities.SQL_EXPLAIN)) {
            throw new IllegalStateException(
                    "compute adapter does not support SQL explain: " + account.adapterId());
        }
        AdapterRequestContext adapterContext = new AdapterRequestContext(
                request.requestContext().requestId(),
                clock.now().plus(adapterTimeout),
                request.requestContext().attributes()
        );
        com.datausher.integration.compute.api.SqlExplainPlan plan =
                invocationExecutor.execute(
                        adapterContext,
                        adapter,
                        "explain",
                        () -> adapter.explain(adapterContext, new SqlExecutionRequest(
                                account.credentialBindingId(),
                                request.statement(),
                                request.parameters().entrySet().stream()
                                        .collect(Collectors.toUnmodifiableMap(
                                                Map.Entry::getKey,
                                                entry -> SqlExecutionValueMapper.toIntegration(
                                                        entry.getValue())
                                        )),
                                1,
                                request.options()
                        ))
                );
        Objects.requireNonNull(plan, "SQL compute adapter returned a null explain plan");
        return new SqlExplainPlan(plan.format(), plan.content(), plan.attributes());
    }
}
