package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskInstanceId;
import com.datausher.workflow.api.WorkflowInstanceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWorkflowRuntimeStore implements WorkflowRuntimeStore {
    private final Map<WorkflowInstanceId, StoredWorkflowRun> runs = new HashMap<>();
    private final Map<String, WorkflowInstanceId> idempotencyIndex = new HashMap<>();
    private final Map<TaskInstanceId, WorkflowInstanceId> taskIndex = new HashMap<>();

    @Override
    public synchronized WorkflowRunCreateResult createOrFind(StoredWorkflowRun run) {
        WorkflowInstanceId existingId = idempotencyIndex.get(run.instance().idempotencyKey());
        if (existingId != null) {
            return new WorkflowRunCreateResult(runs.get(existingId), false);
        }
        if (runs.putIfAbsent(run.instance().instanceId(), run) != null) {
            throw new IllegalStateException("workflow instance already exists: " + run.instance().instanceId());
        }
        idempotencyIndex.put(run.instance().idempotencyKey(), run.instance().instanceId());
        run.tasks().forEach(task -> taskIndex.put(task.taskInstanceId(), run.instance().instanceId()));
        return new WorkflowRunCreateResult(run, true);
    }

    @Override
    public synchronized Optional<StoredWorkflowRun> find(WorkflowInstanceId instanceId) {
        return Optional.ofNullable(runs.get(instanceId));
    }

    @Override
    public synchronized Optional<StoredWorkflowRun> findByTaskInstanceId(TaskInstanceId taskInstanceId) {
        WorkflowInstanceId instanceId = taskIndex.get(taskInstanceId);
        return instanceId == null ? Optional.empty() : Optional.ofNullable(runs.get(instanceId));
    }

    @Override
    public synchronized void update(StoredWorkflowRun expected, StoredWorkflowRun replacement) {
        if (!expected.instance().instanceId().equals(replacement.instance().instanceId())) {
            throw new IllegalArgumentException("workflow instance IDs must match");
        }
        if (!runs.replace(expected.instance().instanceId(), expected, replacement)) {
            throw new IllegalStateException("workflow instance changed concurrently");
        }
    }
}
