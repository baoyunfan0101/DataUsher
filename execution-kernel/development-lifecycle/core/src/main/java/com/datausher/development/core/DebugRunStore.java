package com.datausher.development.core;

import com.datausher.development.api.DebugRun;
import com.datausher.development.api.DebugRunId;
import com.datausher.execution.api.ExecutionRequestId;

import java.time.Instant;
import java.util.Optional;

public interface DebugRunStore {
    DebugRunCreateResult createOrFind(DebugRun debugRun);

    DebugRunTransitionResult markSubmitted(
            DebugRun expectedDebugRun,
            ExecutionRequestId executionRequestId,
            Instant updatedAt
    );

    Optional<DebugRun> find(DebugRunId debugRunId);
}
