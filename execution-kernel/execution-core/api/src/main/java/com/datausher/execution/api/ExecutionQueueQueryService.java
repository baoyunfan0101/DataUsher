package com.datausher.execution.api;

import java.util.List;
import java.util.Optional;

public interface ExecutionQueueQueryService {
    Optional<ExecutionQueue> findQueue(ExecutionQueueId queueId);

    List<ExecutionQueue> listQueues();
}
