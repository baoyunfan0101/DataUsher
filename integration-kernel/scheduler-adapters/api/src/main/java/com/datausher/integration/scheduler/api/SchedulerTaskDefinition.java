package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.IntegrationValue;

import java.util.Map;
import java.util.Objects;

public record SchedulerTaskDefinition(
        String taskKey,
        SchedulerTaskType taskType,
        String payload,
        Map<String, IntegrationValue> parameters,
        Map<String, String> options
) {
    public SchedulerTaskDefinition {
        taskKey = IntegrationIdentifiers.normalize(taskKey, "taskKey");
        taskType = Objects.requireNonNull(taskType, "taskType must not be null");
        payload = IntegrationIdentifiers.requireText(payload, "payload");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
