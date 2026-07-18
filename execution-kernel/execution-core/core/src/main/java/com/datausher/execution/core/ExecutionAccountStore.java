package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionAccount;
import com.datausher.execution.api.ExecutionAccountId;

import java.util.List;
import java.util.Optional;

public interface ExecutionAccountStore {
    void create(ExecutionAccount account);

    void update(ExecutionAccount expected, ExecutionAccount updated);

    Optional<ExecutionAccount> find(ExecutionAccountId accountId);

    List<ExecutionAccount> list();
}
