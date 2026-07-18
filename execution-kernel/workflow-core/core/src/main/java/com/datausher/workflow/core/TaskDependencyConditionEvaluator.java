package com.datausher.workflow.core;

import com.datausher.workflow.api.TaskDependencyCondition;
import com.datausher.workflow.api.TaskInstance;

public interface TaskDependencyConditionEvaluator {
    TaskDependencyCondition condition();

    boolean satisfied(TaskInstance upstreamTask);
}
