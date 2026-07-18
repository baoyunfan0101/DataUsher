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
        List<SchedulerTaskDefinition> tasks,
        List<SchedulerTaskDependency> dependencies,
        Optional<SchedulerSchedule> schedule,
        Map<String, String> options
) {
    public WorkflowDefinition {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        workflowId = IntegrationIdentifiers.normalize(workflowId, "workflowId");
        tasks = List.copyOf(tasks);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        schedule = schedule == null ? Optional.empty() : schedule;
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
    }
}
