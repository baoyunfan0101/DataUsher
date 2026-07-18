package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionQueue;
import com.datausher.execution.api.ExecutionQueueId;

import java.util.List;
import java.util.Optional;

public interface ExecutionQueueStore {
    void create(ExecutionQueue queue);

    void update(ExecutionQueue expected, ExecutionQueue updated);

    Optional<ExecutionQueue> find(ExecutionQueueId queueId);

    List<ExecutionQueue> list();
}
