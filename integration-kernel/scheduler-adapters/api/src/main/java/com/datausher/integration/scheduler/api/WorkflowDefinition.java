package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WorkflowDefinition(
        String bindingId,
        String workflowId,
        long revision,
        String idempotencyKey,
        List<SchedulerTaskDefinition> tasks,
        List<SchedulerTaskDependency> dependencies,
        List<SchedulerSchedule> schedules,
        Map<String, String> options
) {
    public WorkflowDefinition {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workflowId = IntegrationIdentifiers.normalize(workflowId, "workflowId");
        idempotencyKey = IntegrationIdentifiers.requireText(idempotencyKey, "idempotencyKey");
        tasks = List.copyOf(tasks);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        schedules = schedules == null ? List.of() : List.copyOf(schedules);
        options = options == null ? Map.of() : Map.copyOf(options);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks must not be empty");
        }
        var taskKeys = new HashSet<>(tasks.stream().map(SchedulerTaskDefinition::taskKey).toList());
        if (taskKeys.size() != tasks.size()) {
            throw new IllegalArgumentException("task keys must be unique");
        }
        for (SchedulerTaskDependency dependency : dependencies) {
            if (!taskKeys.contains(dependency.upstreamTaskKey())
                    || !taskKeys.contains(dependency.downstreamTaskKey())) {
                throw new IllegalArgumentException("dependency must reference defined tasks");
            }
        }
        if (new HashSet<>(schedules.stream().map(SchedulerSchedule::scheduleId).toList()).size()
                != schedules.size()) {
            throw new IllegalArgumentException("schedule IDs must be unique");
        }
    }

    public WorkflowDefinition(
            String bindingId,
            String workflowId,
            long revision,
            String idempotencyKey,
            List<SchedulerTaskDefinition> tasks,
            List<SchedulerTaskDependency> dependencies,
            Optional<SchedulerSchedule> schedule,
            Map<String, String> options
    ) {
        this(bindingId, workflowId, revision, idempotencyKey, tasks, dependencies,
                schedule == null ? List.of() : schedule.stream().toList(), options);
    }
}
