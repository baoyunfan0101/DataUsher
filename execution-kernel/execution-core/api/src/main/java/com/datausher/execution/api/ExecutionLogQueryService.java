package com.datausher.execution.api;

public interface ExecutionLogQueryService {
    ExecutionLogPage read(ExecutionInstanceId instanceId, long afterSequence, int limit);
}
