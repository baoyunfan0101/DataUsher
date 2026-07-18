package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskRetryStrategy;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TaskRetryStrategyRegistry {
    private final Map<String, TaskRetryDelayCalculator> calculators;

    public TaskRetryStrategyRegistry(Collection<? extends TaskRetryDelayCalculator> calculators) {
        Map<String, TaskRetryDelayCalculator> indexed = new HashMap<>();
        for (TaskRetryDelayCalculator calculator : Objects.requireNonNull(
                calculators, "calculators must not be null")) {
            TaskRetryDelayCalculator existing = indexed.putIfAbsent(
                    calculator.strategyType(), calculator);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate retry strategy calculator: " + calculator.strategyType());
            }
        }
        this.calculators = Map.copyOf(indexed);
    }

    public static TaskRetryStrategyRegistry standard() {
        return new TaskRetryStrategyRegistry(List.of(
                calculator(TaskRetryStrategy.NONE_TYPE, (strategy, attempt) -> Duration.ZERO),
                calculator(TaskRetryStrategy.FIXED_TYPE, (strategy, attempt) ->
                        Duration.parse(strategy.options().get("delay")))));
    }

    public Duration delay(TaskRetryStrategy strategy, int nextAttempt) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        if (nextAttempt < 2) {
            throw new IllegalArgumentException("nextAttempt must be at least two");
        }
        TaskRetryDelayCalculator calculator = calculators.get(strategy.type());
        if (calculator == null) {
            throw new IllegalStateException("retry strategy is not supported: " + strategy.type());
        }
        Duration delay = Objects.requireNonNull(
                calculator.delay(strategy, nextAttempt), "retry delay must not be null");
        if (delay.isNegative()) {
            throw new IllegalStateException("retry delay must not be negative");
        }
        return delay;
    }

    private static TaskRetryDelayCalculator calculator(
            String type,
            java.util.function.BiFunction<TaskRetryStrategy, Integer, Duration> function
    ) {
        return new TaskRetryDelayCalculator() {
            @Override
            public String strategyType() {
                return type;
            }

            @Override
            public Duration delay(TaskRetryStrategy strategy, int nextAttempt) {
                return function.apply(strategy, nextAttempt);
            }
        };
    }
}
