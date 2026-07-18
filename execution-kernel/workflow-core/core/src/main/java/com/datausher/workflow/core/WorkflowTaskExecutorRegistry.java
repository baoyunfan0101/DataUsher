package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowTaskType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class WorkflowTaskExecutorRegistry {
    private final Map<WorkflowTaskType, WorkflowTaskExecutor> executors;

    public WorkflowTaskExecutorRegistry(Collection<? extends WorkflowTaskExecutor> executors) {
        Map<WorkflowTaskType, WorkflowTaskExecutor> indexed = new HashMap<>();
        for (WorkflowTaskExecutor executor : Objects.requireNonNull(
                executors, "executors must not be null")) {
            WorkflowTaskExecutor existing = indexed.putIfAbsent(executor.taskType(), executor);
            if (existing != null) {
                throw new IllegalArgumentException("duplicate workflow task executor: " + executor.taskType());
            }
        }
        this.executors = Map.copyOf(indexed);
    }

    public WorkflowTaskExecutor require(WorkflowTaskType taskType) {
        WorkflowTaskExecutor executor = executors.get(
                Objects.requireNonNull(taskType, "taskType must not be null"));
        if (executor == null) {
            throw new IllegalStateException("workflow task type is not supported: " + taskType.value());
        }
        return executor;
    }
}
