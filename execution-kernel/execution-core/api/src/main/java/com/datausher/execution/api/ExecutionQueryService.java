package com.datausher.execution.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;
import java.util.Optional;

public interface ExecutionQueryService {
    Optional<ExecutionRequest> findRequest(ExecutionRequestId requestId);

    Optional<ExecutionInstance> findInstance(ExecutionInstanceId instanceId);

    List<ExecutionInstance> listInstances(ExecutionRequestId requestId);

    PageResult<ExecutionRequest> search(ExecutionQuery query, PageRequest pageRequest);
}
