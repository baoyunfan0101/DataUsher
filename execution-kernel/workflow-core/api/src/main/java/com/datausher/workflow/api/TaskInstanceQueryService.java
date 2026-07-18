package com.datausher.workflow.api;

import java.util.List;
import java.util.Optional;

public interface TaskInstanceQueryService {
    Optional<TaskInstance> findTaskInstance(TaskInstanceId taskInstanceId);

    List<TaskInstance> listTaskInstances(WorkflowInstanceId workflowInstanceId);
}
