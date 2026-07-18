package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExecutionStore {
    void create(StoredExecution execution);

    Optional<ExecutionDispatch> claimNext(
            ExecutionQueueId queueId,
            int maxConcurrency,
            ExecutionInstanceId instanceId,
            Instant claimedAt
    );

    void update(StoredExecution expected, StoredExecution updated);

    void update(
            StoredExecution expectedExecution,
            StoredExecution updatedExecution,
            StoredExecutionInstance expectedInstance,
            StoredExecutionInstance updatedInstance
    );

    Optional<StoredExecution> find(ExecutionRequestId requestId);

    Optional<StoredExecutionInstance> findInstance(ExecutionInstanceId instanceId);

    List<StoredExecutionInstance> listInstances(ExecutionRequestId requestId);

    PageResult<com.datausher.execution.api.ExecutionRequest> search(
            ExecutionQuery query,
            PageRequest pageRequest
    );
}
