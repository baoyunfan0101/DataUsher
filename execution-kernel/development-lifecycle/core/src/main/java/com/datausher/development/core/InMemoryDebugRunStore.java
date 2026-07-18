package com.datausher.development.core;

import com.datausher.development.api.DebugRun;
import com.datausher.development.api.DebugRunId;
import com.datausher.development.api.DebugRunState;
import com.datausher.execution.api.ExecutionRequestId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryDebugRunStore implements DebugRunStore {
    private final Map<DebugRunId, DebugRun> runs = new HashMap<>();
    private final Map<String, DebugRunId> idempotencyIndex = new HashMap<>();

    @Override
    public synchronized DebugRunCreateResult createOrFind(DebugRun debugRun) {
        DebugRunId existingId = idempotencyIndex.get(debugRun.idempotencyKey());
        if (existingId != null) {
            return new DebugRunCreateResult(runs.get(existingId), false);
        }
        if (runs.putIfAbsent(debugRun.debugRunId(), debugRun) != null) {
            throw new IllegalStateException("debug run already exists: " + debugRun.debugRunId());
        }
        idempotencyIndex.put(debugRun.idempotencyKey(), debugRun.debugRunId());
        return new DebugRunCreateResult(debugRun, true);
    }

    @Override
    public synchronized DebugRun markSubmitted(
            DebugRun expectedDebugRun,
            ExecutionRequestId executionRequestId,
            Instant updatedAt
    ) {
        DebugRun current = runs.get(expectedDebugRun.debugRunId());
        if (current == null) {
            throw new IllegalArgumentException("debug run does not exist: " + expectedDebugRun.debugRunId());
        }
        if (!current.equals(expectedDebugRun)) {
            if (current.state() == DebugRunState.SUBMITTED
                    && current.executionRequestId().orElseThrow().equals(executionRequestId)) {
                return current;
            }
            throw new IllegalStateException("debug run changed concurrently: " + current.debugRunId());
        }
        DebugRun submitted = new DebugRun(
                current.debugRunId(), current.scriptId(), current.scriptVersion(),
                current.idempotencyKey(), current.parameters(), DebugRunState.SUBMITTED,
                Optional.of(executionRequestId), current.createdAt(), updatedAt,
                current.revision() + 1);
        runs.put(current.debugRunId(), submitted);
        return submitted;
    }

    @Override
    public synchronized Optional<DebugRun> find(DebugRunId debugRunId) {
        return Optional.ofNullable(runs.get(debugRunId));
    }
}
