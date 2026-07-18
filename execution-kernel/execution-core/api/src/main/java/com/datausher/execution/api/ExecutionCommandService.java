package com.datausher.execution.api;

public interface ExecutionCommandService {
    ExecutionRequest submit(SubmitExecutionRequest request);

    ExecutionRequest cancel(CancelExecutionRequest request);
}
