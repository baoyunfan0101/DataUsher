package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionInstance;
import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionState;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryExecutionStore implements ExecutionStore {
    private final Map<ExecutionRequestId, StoredExecution> executions = new HashMap<>();
    private final Map<String, ExecutionRequestId> idempotencyIndex = new HashMap<>();
    private final Map<ExecutionInstanceId, StoredExecutionInstance> instances = new HashMap<>();

    @Override
    public synchronized ExecutionCreateResult create(StoredExecution execution) {
        String idempotencyKey = execution.request().idempotencyKey();
        ExecutionRequestId existingId = idempotencyIndex.get(idempotencyKey);
        if (existingId != null) {
            return new ExecutionCreateResult(executions.get(existingId), false);
        }
        if (executions.putIfAbsent(execution.request().requestId(), execution) != null) {
            throw new IllegalStateException(
                    "execution request already exists: " + execution.request().requestId());
        }
        idempotencyIndex.put(idempotencyKey, execution.request().requestId());
        return new ExecutionCreateResult(execution, true);
    }

    @Override
    public synchronized Optional<ExecutionDispatch> claimNext(
            ExecutionQueueId queueId,
            int maxConcurrency,
            ExecutionInstanceId instanceId,
            Instant claimedAt
    ) {
        long active = instances.values().stream()
                .map(StoredExecutionInstance::instance)
                .filter(instance -> !instance.state().terminal())
                .map(instance -> executions.get(instance.requestId()))
                .filter(execution -> execution != null
                        && execution.request().queueId().equals(queueId))
                .count();
        if (active >= maxConcurrency) {
            return Optional.empty();
        }
        Optional<StoredExecution> candidate = executions.values().stream()
                .filter(execution -> execution.request().queueId().equals(queueId))
                .filter(execution -> execution.request().state() == ExecutionState.QUEUED)
                .sorted(Comparator
                        .comparing((StoredExecution value) -> value.request().submittedAt())
                        .thenComparing(value -> value.request().requestId()))
                .findFirst();
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        StoredExecution current = candidate.get();
        ExecutionRequest claimedRequest = transitionRequest(
                current.request(), ExecutionState.DISPATCHING, claimedAt,
                Optional.empty());
        StoredExecution claimed = new StoredExecution(
                claimedRequest, current.requestContext());
        int attempt = (int) instances.values().stream()
                .filter(value -> value.instance().requestId().equals(
                        current.request().requestId()))
                .count() + 1;
        StoredExecutionInstance instance = new StoredExecutionInstance(
                new ExecutionInstance(
                        instanceId,
                        current.request().requestId(),
                        attempt,
                        ExecutionState.DISPATCHING,
                        claimedAt,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        1
                ),
                Optional.empty()
        );
        executions.put(current.request().requestId(), claimed);
        if (instances.putIfAbsent(instanceId, instance) != null) {
            executions.put(current.request().requestId(), current);
            throw new IllegalStateException("execution instance already exists: " + instanceId);
        }
        return Optional.of(new ExecutionDispatch(claimed, instance));
    }

    @Override
    public synchronized void update(StoredExecution expected, StoredExecution updated) {
        requireSameRequest(expected, updated);
        if (!executions.replace(expected.request().requestId(), expected, updated)) {
            throw new IllegalStateException(
                    "execution request changed concurrently: " + expected.request().requestId());
        }
    }

    @Override
    public synchronized void update(
            StoredExecution expectedExecution,
            StoredExecution updatedExecution,
            StoredExecutionInstance expectedInstance,
            StoredExecutionInstance updatedInstance
    ) {
        requireSameRequest(expectedExecution, updatedExecution);
        if (!expectedInstance.instance().instanceId().equals(
                updatedInstance.instance().instanceId())) {
            throw new IllegalArgumentException("execution instance IDs must match");
        }
        StoredExecution currentExecution = executions.get(
                expectedExecution.request().requestId());
        StoredExecutionInstance currentInstance = instances.get(
                expectedInstance.instance().instanceId());
        if (!expectedExecution.equals(currentExecution)
                || !expectedInstance.equals(currentInstance)) {
            throw new IllegalStateException("execution changed concurrently");
        }
        executions.put(updatedExecution.request().requestId(), updatedExecution);
        instances.put(updatedInstance.instance().instanceId(), updatedInstance);
    }

    @Override
    public synchronized Optional<StoredExecution> find(ExecutionRequestId requestId) {
        return Optional.ofNullable(executions.get(requestId));
    }

    @Override
    public synchronized Optional<StoredExecution> findByIdempotencyKey(String idempotencyKey) {
        ExecutionRequestId requestId = idempotencyIndex.get(idempotencyKey);
        return requestId == null ? Optional.empty() : Optional.ofNullable(executions.get(requestId));
    }

    @Override
    public synchronized Optional<StoredExecutionInstance> findInstance(
            ExecutionInstanceId instanceId
    ) {
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public synchronized List<StoredExecutionInstance> listInstances(
            ExecutionRequestId requestId
    ) {
        return instances.values().stream()
                .filter(value -> value.instance().requestId().equals(requestId))
                .sorted(Comparator.comparingInt(value -> value.instance().attempt()))
                .toList();
    }

    @Override
    public synchronized PageResult<ExecutionRequest> search(
            ExecutionQuery query,
            PageRequest pageRequest
    ) {
        List<ExecutionRequest> matches = executions.values().stream()
                .map(StoredExecution::request)
                .filter(request -> query.states().isEmpty()
                        || query.states().contains(request.state()))
                .filter(request -> query.workloadType().isEmpty()
                        || query.workloadType().get().equals(request.workload().type()))
                .filter(request -> query.queueId().isEmpty()
                        || query.queueId().get().equals(request.queueId()))
                .filter(request -> query.accountId().isEmpty()
                        || query.accountId().get().equals(request.accountId()))
                .filter(request -> query.submittedFrom().isEmpty()
                        || !request.submittedAt().isBefore(query.submittedFrom().get()))
                .filter(request -> query.submittedUntil().isEmpty()
                        || request.submittedAt().isBefore(query.submittedUntil().get()))
                .sorted(Comparator.comparing(ExecutionRequest::submittedAt).reversed()
                        .thenComparing(ExecutionRequest::requestId))
                .toList();
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(matches.subList(fromIndex, toIndex), matches.size(),
                pageRequest.page(), pageRequest.size());
    }

    private static void requireSameRequest(
            StoredExecution expected,
            StoredExecution updated
    ) {
        if (!expected.request().requestId().equals(updated.request().requestId())) {
            throw new IllegalArgumentException("execution request IDs must match");
        }
    }

    static ExecutionRequest transitionRequest(
            ExecutionRequest request,
            ExecutionState state,
            Instant changedAt,
            Optional<com.datausher.execution.api.ExecutionFailure> failure
    ) {
        return new ExecutionRequest(
                request.requestId(), request.specification(), request.idempotencyKey(), request.origin(),
                state, request.submittedAt(),
                changedAt, failure, request.revision() + 1);
    }
}
