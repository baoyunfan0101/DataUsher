package com.datausher.workflow.api;

import java.util.Locale;
import java.util.Objects;

public record TaskDependencyCondition(String value) {
    public static final TaskDependencyCondition ON_SUCCESS = new TaskDependencyCondition("on-success");
    public static final TaskDependencyCondition ON_FAILURE = new TaskDependencyCondition("on-failure");
    public static final TaskDependencyCondition ALWAYS = new TaskDependencyCondition("always");

    public TaskDependencyCondition {
        value = Objects.requireNonNull(value, "value must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value must match [a-z][a-z0-9.-]{0,126}");
        }
    }
}
