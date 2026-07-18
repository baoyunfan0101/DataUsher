package com.datausher.execution.api;

public interface ExecutionAccountCommandService {
    ExecutionAccount register(RegisterExecutionAccountRequest request);

    ExecutionAccount changeStatus(ChangeExecutionAccountStatusRequest request);
}
