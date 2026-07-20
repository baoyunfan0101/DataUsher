package com.datausher.data.quality.core;

import com.datausher.data.quality.api.AssessmentExecutionType;
import com.datausher.execution.api.ExecutionWorkload;

public interface QualityExecutionPlanner {
    AssessmentExecutionType executionType();

    ExecutionWorkload plan(QualityCheckPlanningRequest request);
}
