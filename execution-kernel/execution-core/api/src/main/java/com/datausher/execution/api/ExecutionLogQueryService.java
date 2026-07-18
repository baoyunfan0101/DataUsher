package com.datausher.execution.api;

public interface ExecutionLogQueryService {
    ExecutionLogPage read(ReadExecutionLogRequest request);
}
