package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskInstanceId;
import com.datausher.workflow.api.WorkflowInstanceId;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryWorkflowRuntimeStore implements WorkflowRuntimeStore {
    private final Map<WorkflowInstanceId, StoredWorkflowRun> runs = new HashMap<>();
    private final Map<String, WorkflowInstanceId> idempotencyIndex = new HashMap<>();
    private final Map<TaskInstanceId, WorkflowInstanceId> taskIndex = new HashMap<>();
    private final Map<WorkflowInstanceId, WorkflowRunLease> leases = new HashMap<>();

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
    public synchronized List<WorkflowRunLease> claimRunnable(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int limit
    ) {
        String normalizedWorkerId = requireWorkerId(workerId);
        requireLeaseArguments(now, leaseDuration, limit);
        leases.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        return runs.values().stream()
                .filter(run -> !run.instance().state().terminal())
                .filter(run -> !leases.containsKey(run.instance().instanceId()))
                .sorted(Comparator
                        .comparing((StoredWorkflowRun run) -> run.instance().createdAt())
                        .thenComparing(run -> run.instance().instanceId().value()))
                .limit(limit)
                .map(run -> {
                    WorkflowRunLease lease = new WorkflowRunLease(
                            run.instance().instanceId(), UUID.randomUUID().toString(),
                            normalizedWorkerId, now, now.plus(leaseDuration));
                    leases.put(run.instance().instanceId(), lease);
                    return lease;
                })
                .toList();
    }

    @Override
    public synchronized Optional<StoredWorkflowRun> findClaimed(
            WorkflowRunLease lease,
            Instant now
    ) {
        WorkflowRunLease current = leases.get(lease.instanceId());
        if (current == null || !current.equals(lease) || !current.expiresAt().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.ofNullable(runs.get(lease.instanceId()));
    }

    @Override
    public synchronized WorkflowRunLease renew(
            WorkflowRunLease lease,
            Instant now,
            Duration leaseDuration
    ) {
        requireLeaseArguments(now, leaseDuration, 1);
        WorkflowRunLease current = leases.get(lease.instanceId());
        if (current == null || !current.equals(lease) || !current.expiresAt().isAfter(now)) {
            throw new WorkflowRunLeaseLostException(
                    "workflow run lease is no longer owned: " + lease.instanceId().value());
        }
        WorkflowRunLease renewed = new WorkflowRunLease(
                current.instanceId(), current.leaseToken(), current.workerId(),
                current.acquiredAt(), now.plus(leaseDuration));
        leases.put(renewed.instanceId(), renewed);
        return renewed;
    }

    @Override
    public synchronized void release(WorkflowRunLease lease) {
        leases.remove(lease.instanceId(), lease);
    }

    @Override
    public synchronized void update(StoredWorkflowRun expected, StoredWorkflowRun replacement) {
        if (!expected.instance().instanceId().equals(replacement.instance().instanceId())) {
            throw new IllegalArgumentException("workflow instance IDs must match");
        }
        if (!runs.replace(expected.instance().instanceId(), expected, replacement)) {
            StoredWorkflowRun actual = runs.get(expected.instance().instanceId());
            if (actual != null) {
                throw new RevisionConflictException(
                        "workflow-instance", expected.instance().instanceId().value(),
                        expected.instance().revision(), actual.instance().revision());
            }
            throw new IllegalStateException("workflow instance no longer exists");
        }
    }

    private static String requireWorkerId(String workerId) {
        String normalized = java.util.Objects.requireNonNull(
                workerId, "workerId must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        return normalized;
    }

    private static void requireLeaseArguments(
            Instant now,
            Duration leaseDuration,
            int limit
    ) {
        java.util.Objects.requireNonNull(now, "now must not be null");
        java.util.Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        if (leaseDuration.isZero() || leaseDuration.isNegative() || limit < 1) {
            throw new IllegalArgumentException("positive leaseDuration and limit are required");
        }
    }
}
