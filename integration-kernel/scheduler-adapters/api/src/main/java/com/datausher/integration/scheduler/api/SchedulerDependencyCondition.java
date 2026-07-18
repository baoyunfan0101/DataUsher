package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record SchedulerDependencyCondition(String value) {
    public static final SchedulerDependencyCondition ON_SUCCESS = new SchedulerDependencyCondition("on-success");
    public static final SchedulerDependencyCondition ON_FAILURE = new SchedulerDependencyCondition("on-failure");
    public static final SchedulerDependencyCondition ALWAYS = new SchedulerDependencyCondition("always");

    public SchedulerDependencyCondition {
        value = IntegrationIdentifiers.normalize(value, "value");
    }
}
