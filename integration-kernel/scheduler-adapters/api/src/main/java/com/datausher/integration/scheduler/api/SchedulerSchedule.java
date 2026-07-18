package com.datausher.integration.scheduler.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

public record SchedulerSchedule(
        SchedulerScheduleType type,
        String expression,
        ZoneId zoneId,
        Map<String, String> options
) {
    public SchedulerSchedule {
        type = Objects.requireNonNull(type, "type must not be null");
        expression = IntegrationIdentifiers.requireText(expression, "expression");
        zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
