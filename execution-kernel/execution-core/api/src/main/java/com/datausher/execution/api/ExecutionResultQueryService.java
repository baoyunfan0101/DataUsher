package com.datausher.execution.api;

public interface ExecutionResultQueryService {
    ExecutionResultPage read(ExecutionInstanceId instanceId, long offset, int limit);
}
