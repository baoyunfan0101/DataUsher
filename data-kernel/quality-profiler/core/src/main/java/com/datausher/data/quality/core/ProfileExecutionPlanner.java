package com.datausher.data.quality.core;

import com.datausher.data.quality.api.AssessmentExecutionType;
import com.datausher.data.quality.api.ProfileJob;
import com.datausher.execution.api.ExecutionWorkload;

public interface ProfileExecutionPlanner {
    AssessmentExecutionType executionType();

    ExecutionWorkload plan(ProfileJob job);
}
