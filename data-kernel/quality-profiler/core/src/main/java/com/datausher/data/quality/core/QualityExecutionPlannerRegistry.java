package com.datausher.data.quality.core;

import com.datausher.data.quality.api.AssessmentExecutionType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class QualityExecutionPlannerRegistry {
    private final Map<AssessmentExecutionType, QualityExecutionPlanner> planners;

    public QualityExecutionPlannerRegistry(
            Collection<? extends QualityExecutionPlanner> planners
    ) {
        Map<AssessmentExecutionType, QualityExecutionPlanner> indexed = new HashMap<>();
        for (QualityExecutionPlanner planner : Objects.requireNonNull(
                planners, "planners must not be null")) {
            QualityExecutionPlanner existing = indexed.putIfAbsent(
                    planner.executionType(), planner);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate quality execution planner: "
                                + planner.executionType().value());
            }
        }
        this.planners = Map.copyOf(indexed);
    }

    public QualityExecutionPlanner require(AssessmentExecutionType type) {
        QualityExecutionPlanner planner = planners.get(
                Objects.requireNonNull(type, "type must not be null"));
        if (planner == null) {
            throw new IllegalStateException(
                    "quality execution type is not supported: " + type.value());
        }
        return planner;
    }
}
