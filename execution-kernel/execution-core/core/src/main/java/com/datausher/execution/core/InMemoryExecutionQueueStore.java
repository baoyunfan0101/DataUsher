package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionQueue;
import com.datausher.execution.api.ExecutionQueueId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryExecutionQueueStore implements ExecutionQueueStore {
    private final ConcurrentMap<ExecutionQueueId, ExecutionQueue> queues =
            new ConcurrentHashMap<>();

    @Override
    public void create(ExecutionQueue queue) {
        if (queues.putIfAbsent(queue.queueId(), queue) != null) {
            throw new IllegalStateException("execution queue already exists: " + queue.queueId());
        }
    }

    @Override
    public void update(ExecutionQueue expected, ExecutionQueue updated) {
        if (!queues.replace(expected.queueId(), expected, updated)) {
            throw new IllegalStateException(
                    "execution queue changed concurrently: " + expected.queueId());
        }
    }

    @Override
    public Optional<ExecutionQueue> find(ExecutionQueueId queueId) {
        return Optional.ofNullable(queues.get(queueId));
    }

    @Override
    public List<ExecutionQueue> list() {
        return queues.values().stream()
                .sorted(Comparator.comparingInt(ExecutionQueue::priority).reversed()
                        .thenComparing(ExecutionQueue::queueId))
                .toList();
    }
}
