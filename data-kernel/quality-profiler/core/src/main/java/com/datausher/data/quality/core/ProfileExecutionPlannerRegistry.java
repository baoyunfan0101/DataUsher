package com.datausher.data.quality.core;

import com.datausher.data.quality.api.AssessmentExecutionType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProfileExecutionPlannerRegistry {
    private final Map<AssessmentExecutionType, ProfileExecutionPlanner> planners;

    public ProfileExecutionPlannerRegistry(
            Collection<? extends ProfileExecutionPlanner> planners
    ) {
        Map<AssessmentExecutionType, ProfileExecutionPlanner> indexed = new HashMap<>();
        for (ProfileExecutionPlanner planner : Objects.requireNonNull(
                planners, "planners must not be null")) {
            ProfileExecutionPlanner existing = indexed.putIfAbsent(
                    planner.executionType(), planner);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate profile execution planner: "
                                + planner.executionType().value());
            }
        }
        this.planners = Map.copyOf(indexed);
    }

    public ProfileExecutionPlanner require(AssessmentExecutionType type) {
        ProfileExecutionPlanner planner = planners.get(
                Objects.requireNonNull(type, "type must not be null"));
        if (planner == null) {
            throw new IllegalStateException(
                    "profile execution type is not supported: " + type.value());
        }
        return planner;
    }
}
