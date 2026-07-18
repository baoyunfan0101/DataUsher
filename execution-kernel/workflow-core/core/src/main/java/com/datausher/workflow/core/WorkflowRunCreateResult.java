package com.datausher.workflow.core;

import java.util.Objects;

public record WorkflowRunCreateResult(StoredWorkflowRun run, boolean created) {
    public WorkflowRunCreateResult {
        run = Objects.requireNonNull(run, "run must not be null");
    }
}
