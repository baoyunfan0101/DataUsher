package com.datausher.execution.api;

public interface ExecutionQueueCommandService {
    ExecutionQueue create(CreateExecutionQueueRequest request);

    ExecutionQueue changeStatus(ChangeExecutionQueueStatusRequest request);
}
