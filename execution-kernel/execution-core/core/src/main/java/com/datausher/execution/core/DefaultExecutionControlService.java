package com.datausher.execution.core;

import com.datausher.execution.api.ChangeExecutionAccountStatusRequest;
import com.datausher.execution.api.ChangeExecutionQueueStatusRequest;
import com.datausher.execution.api.CreateExecutionQueueRequest;
import com.datausher.execution.api.ExecutionAccount;
import com.datausher.execution.api.ExecutionAccountCommandService;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionAccountQueryService;
import com.datausher.execution.api.ExecutionAccountStatus;
import com.datausher.execution.api.ExecutionQueue;
import com.datausher.execution.api.ExecutionQueueCommandService;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionQueueQueryService;
import com.datausher.execution.api.ExecutionQueueStatus;
import com.datausher.execution.api.RegisterExecutionAccountRequest;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultExecutionControlService implements
        ExecutionQueueCommandService,
        ExecutionQueueQueryService,
        ExecutionAccountCommandService,
        ExecutionAccountQueryService {
    private final ExecutionQueueStore queueStore;
    private final ExecutionAccountStore accountStore;
    private final Clock clock;

    public DefaultExecutionControlService(
            ExecutionQueueStore queueStore,
            ExecutionAccountStore accountStore,
            Clock clock
    ) {
        this.queueStore = Objects.requireNonNull(queueStore, "queueStore must not be null");
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ExecutionQueue create(CreateExecutionQueueRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        ExecutionQueue queue = new ExecutionQueue(
                request.queueId(), request.displayName(), request.maxConcurrency(),
                request.priority(), ExecutionQueueStatus.ACTIVE, request.attributes(),
                now, now, 1);
        queueStore.create(queue);
        return queue;
    }

    @Override
    public ExecutionQueue changeStatus(ChangeExecutionQueueStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ExecutionQueue current = requireQueue(request.queueId());
        requireRevision(current.revision(), request.expectedRevision(), "execution queue");
        if (current.status() == request.status()) {
            return current;
        }
        ExecutionQueue updated = current.withStatus(request.status(), clock.now());
        queueStore.update(current, updated);
        return updated;
    }

    @Override
    public Optional<ExecutionQueue> findQueue(ExecutionQueueId queueId) {
        return queueStore.find(Objects.requireNonNull(queueId, "queueId must not be null"));
    }

    @Override
    public List<ExecutionQueue> listQueues() {
        return queueStore.list();
    }

    @Override
    public ExecutionAccount register(RegisterExecutionAccountRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        ExecutionAccount account = new ExecutionAccount(
                request.accountId(), request.displayName(), request.adapterId(),
                request.credentialBindingId(), request.workloadTypes(),
                ExecutionAccountStatus.ACTIVE, request.attributes(), now, now, 1);
        accountStore.create(account);
        return account;
    }

    @Override
    public ExecutionAccount changeStatus(ChangeExecutionAccountStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ExecutionAccount current = requireAccount(request.accountId());
        requireRevision(current.revision(), request.expectedRevision(), "execution account");
        if (current.status() == request.status()) {
            return current;
        }
        ExecutionAccount updated = current.withStatus(request.status(), clock.now());
        accountStore.update(current, updated);
        return updated;
    }

    @Override
    public Optional<ExecutionAccount> findAccount(ExecutionAccountId accountId) {
        return accountStore.find(Objects.requireNonNull(accountId, "accountId must not be null"));
    }

    @Override
    public List<ExecutionAccount> listAccounts() {
        return accountStore.list();
    }

    private ExecutionQueue requireQueue(ExecutionQueueId queueId) {
        return queueStore.find(queueId).orElseThrow(() ->
                new IllegalArgumentException("execution queue does not exist: " + queueId));
    }

    private ExecutionAccount requireAccount(ExecutionAccountId accountId) {
        return accountStore.find(accountId).orElseThrow(() ->
                new IllegalArgumentException("execution account does not exist: " + accountId));
    }

    private static void requireRevision(long current, long expected, String kind) {
        if (current != expected) {
            throw new IllegalStateException(kind + " revision does not match expectedRevision");
        }
    }
}
