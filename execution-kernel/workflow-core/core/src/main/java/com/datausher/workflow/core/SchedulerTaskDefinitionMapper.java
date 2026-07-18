package com.datausher.workflow.core;

import com.datausher.integration.scheduler.api.SchedulerTaskDefinition;
import com.datausher.workflow.api.WorkflowTaskType;

public interface SchedulerTaskDefinitionMapper {
    WorkflowTaskType taskType();

    SchedulerTaskDefinition map(SchedulerTaskMappingRequest request);
}
