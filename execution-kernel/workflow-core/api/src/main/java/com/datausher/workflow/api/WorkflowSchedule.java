package com.datausher.workflow.api;

import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

public record WorkflowSchedule(
        WorkflowScheduleType type,
        String expression,
        ZoneId zoneId,
        Map<String, String> options
) {
    public WorkflowSchedule {
        type = Objects.requireNonNull(type, "type must not be null");
        expression = Objects.requireNonNull(expression, "expression must not be null").trim();
        zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("expression must not be blank");
        }
    }
}
