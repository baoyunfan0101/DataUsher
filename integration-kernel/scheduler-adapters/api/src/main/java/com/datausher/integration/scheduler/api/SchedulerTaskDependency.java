package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Objects;

public record SchedulerTaskDependency(
        String upstreamTaskKey,
        String downstreamTaskKey,
        SchedulerDependencyCondition condition
) {
    public SchedulerTaskDependency {
        upstreamTaskKey = IntegrationIdentifiers.normalize(upstreamTaskKey, "upstreamTaskKey");
        downstreamTaskKey = IntegrationIdentifiers.normalize(downstreamTaskKey, "downstreamTaskKey");
        condition = Objects.requireNonNull(condition, "condition must not be null");
        if (upstreamTaskKey.equals(downstreamTaskKey)) {
            throw new IllegalArgumentException("scheduler task must not depend on itself");
        }
    }
}
