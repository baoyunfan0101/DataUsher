package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskRetryStrategy;

import java.time.Duration;

public interface TaskRetryDelayCalculator {
    String strategyType();

    Duration delay(TaskRetryStrategy strategy, int nextAttempt);
}
