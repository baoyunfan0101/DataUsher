package com.datausher.workflow.api;

import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

public record WorkflowSchedule(
        WorkflowScheduleId scheduleId,
        WorkflowScheduleType type,
        String expression,
        ZoneId zoneId,
        WorkflowScheduleStatus status,
        Map<String, String> options
) {
    public WorkflowSchedule {
        scheduleId = Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        expression = Objects.requireNonNull(expression, "expression must not be null").trim();
        zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("expression must not be blank");
        }
    }
}
