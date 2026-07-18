package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record SchedulerTaskType(String value) {
    public static final SchedulerTaskType PLATFORM_EXECUTION = new SchedulerTaskType("platform-execution");

    public SchedulerTaskType {
        value = IntegrationIdentifiers.normalize(value, "value");
    }
}
