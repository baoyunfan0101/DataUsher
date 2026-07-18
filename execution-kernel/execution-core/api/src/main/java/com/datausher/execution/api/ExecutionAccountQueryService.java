package com.datausher.execution.api;

import java.util.List;
import java.util.Optional;

public interface ExecutionAccountQueryService {
    Optional<ExecutionAccount> findAccount(ExecutionAccountId accountId);

    List<ExecutionAccount> listAccounts();
}
