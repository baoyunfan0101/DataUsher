package com.datausher.workflow.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record WorkflowVersionSpec(
        List<WorkflowTaskDefinition> tasks,
        List<TaskDependency> dependencies,
        Optional<WorkflowSchedule> schedule,
        Map<String, String> attributes
) {
    public WorkflowVersionSpec {
        tasks = List.copyOf(tasks);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        schedule = schedule == null ? Optional.empty() : schedule;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        validate(tasks, dependencies);
    }

    private static void validate(
            List<WorkflowTaskDefinition> tasks,
            List<TaskDependency> dependencies
    ) {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks must not be empty");
        }
        Set<String> keys = new HashSet<>(tasks.stream()
                .map(WorkflowTaskDefinition::taskKey).toList());
        if (keys.size() != tasks.size()) {
            throw new IllegalArgumentException("task keys must be unique");
        }
        Map<String, Set<String>> prerequisites = new HashMap<>();
        keys.forEach(key -> prerequisites.put(key, new HashSet<>()));
        Set<String> edges = new HashSet<>();
        for (TaskDependency dependency : dependencies) {
            if (!keys.contains(dependency.upstreamTaskKey())
                    || !keys.contains(dependency.downstreamTaskKey())) {
                throw new IllegalArgumentException("dependency must reference defined tasks");
            }
            String edge = dependency.upstreamTaskKey().length() + ":"
                    + dependency.upstreamTaskKey() + dependency.downstreamTaskKey();
            if (!edges.add(edge)) {
                throw new IllegalArgumentException("task dependency pair must be unique");
            }
            prerequisites.get(dependency.downstreamTaskKey()).add(dependency.upstreamTaskKey());
        }
        Set<String> resolved = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (String key : keys) {
                if (!resolved.contains(key) && resolved.containsAll(prerequisites.get(key))) {
                    resolved.add(key);
                    changed = true;
                }
            }
        } while (changed);
        if (resolved.size() != keys.size()) {
            throw new IllegalArgumentException("task dependencies must be acyclic");
        }
    }
}
