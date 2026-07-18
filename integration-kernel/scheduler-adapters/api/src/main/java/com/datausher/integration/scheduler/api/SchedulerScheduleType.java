package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

public record SchedulerScheduleType(String value) {
    public static final SchedulerScheduleType CRON = new SchedulerScheduleType("cron");

    public SchedulerScheduleType {
        value = IntegrationIdentifiers.normalize(value, "value");
    }
}
