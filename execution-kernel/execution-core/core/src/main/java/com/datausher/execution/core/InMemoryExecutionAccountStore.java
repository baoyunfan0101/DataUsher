package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionAccount;
import com.datausher.execution.api.ExecutionAccountId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryExecutionAccountStore implements ExecutionAccountStore {
    private final ConcurrentMap<ExecutionAccountId, ExecutionAccount> accounts =
            new ConcurrentHashMap<>();

    @Override
    public void create(ExecutionAccount account) {
        if (accounts.putIfAbsent(account.accountId(), account) != null) {
            throw new IllegalStateException(
                    "execution account already exists: " + account.accountId());
        }
    }

    @Override
    public void update(ExecutionAccount expected, ExecutionAccount updated) {
        if (!accounts.replace(expected.accountId(), expected, updated)) {
            throw new IllegalStateException(
                    "execution account changed concurrently: " + expected.accountId());
        }
    }

    @Override
    public Optional<ExecutionAccount> find(ExecutionAccountId accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public List<ExecutionAccount> list() {
        return accounts.values().stream()
                .sorted(Comparator.comparing(ExecutionAccount::accountId))
                .toList();
    }
}
