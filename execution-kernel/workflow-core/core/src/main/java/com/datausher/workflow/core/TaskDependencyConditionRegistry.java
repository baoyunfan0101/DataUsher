package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.TaskInstance;
import com.datausher.workflow.api.TaskInstanceState;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TaskDependencyConditionRegistry {
    private final Map<TaskDependencyCondition, TaskDependencyConditionEvaluator> evaluators;

    public TaskDependencyConditionRegistry(
            Collection<? extends TaskDependencyConditionEvaluator> evaluators
    ) {
        Map<TaskDependencyCondition, TaskDependencyConditionEvaluator> indexed = new HashMap<>();
        for (TaskDependencyConditionEvaluator evaluator : Objects.requireNonNull(
                evaluators, "evaluators must not be null")) {
            TaskDependencyConditionEvaluator existing = indexed.putIfAbsent(
                    evaluator.condition(), evaluator);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate dependency condition evaluator: " + evaluator.condition());
            }
        }
        this.evaluators = Map.copyOf(indexed);
    }

    public static TaskDependencyConditionRegistry standard() {
        return new TaskDependencyConditionRegistry(List.of(
                evaluator(TaskDependencyCondition.ALWAYS, task -> true),
                evaluator(TaskDependencyCondition.ON_SUCCESS,
                        task -> task.state() == TaskInstanceState.SUCCEEDED),
                evaluator(TaskDependencyCondition.ON_FAILURE,
                        task -> task.state() == TaskInstanceState.FAILED
                                || task.state() == TaskInstanceState.TIMED_OUT
                                || task.state() == TaskInstanceState.CANCELLED)));
    }

    public boolean satisfied(TaskDependencyCondition condition, TaskInstance upstreamTask) {
        TaskDependencyConditionEvaluator evaluator = evaluators.get(
                Objects.requireNonNull(condition, "condition must not be null"));
        if (evaluator == null) {
            throw new IllegalStateException(
                    "dependency condition is not supported: " + condition.value());
        }
        return evaluator.satisfied(Objects.requireNonNull(
                upstreamTask, "upstreamTask must not be null"));
    }

    private static TaskDependencyConditionEvaluator evaluator(
            TaskDependencyCondition condition,
            java.util.function.Predicate<TaskInstance> predicate
    ) {
        return new TaskDependencyConditionEvaluator() {
            @Override
            public TaskDependencyCondition condition() {
                return condition;
            }

            @Override
            public boolean satisfied(TaskInstance upstreamTask) {
                return predicate.test(upstreamTask);
            }
        };
    }
}
