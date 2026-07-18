package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowTaskType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SchedulerTaskDefinitionMapperRegistry {
    private final Map<WorkflowTaskType, SchedulerTaskDefinitionMapper> mappers;

    public SchedulerTaskDefinitionMapperRegistry(
            Collection<? extends SchedulerTaskDefinitionMapper> mappers
    ) {
        Map<WorkflowTaskType, SchedulerTaskDefinitionMapper> indexed = new HashMap<>();
        for (SchedulerTaskDefinitionMapper mapper : Objects.requireNonNull(
                mappers, "mappers must not be null")) {
            SchedulerTaskDefinitionMapper existing = indexed.putIfAbsent(mapper.taskType(), mapper);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate scheduler task mapper: " + mapper.taskType().value());
            }
        }
        this.mappers = Map.copyOf(indexed);
    }

    public SchedulerTaskDefinitionMapper require(WorkflowTaskType taskType) {
        SchedulerTaskDefinitionMapper mapper = mappers.get(
                Objects.requireNonNull(taskType, "taskType must not be null"));
        if (mapper == null) {
            throw new IllegalStateException(
                    "workflow task type cannot be published: " + taskType.value());
        }
        return mapper;
    }
}
