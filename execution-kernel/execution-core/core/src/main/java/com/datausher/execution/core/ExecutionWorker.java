package com.datausher.execution.core;

import com.datausher.execution.api.ExecutionInstance;
import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Optional;

public interface ExecutionWorker {
    Optional<ExecutionInstance> dispatchNext(
            ExecutionQueueId queueId,
            RequestContext requestContext
    );

    ExecutionInstance refresh(
            ExecutionInstanceId instanceId,
            RequestContext requestContext
    );
}
