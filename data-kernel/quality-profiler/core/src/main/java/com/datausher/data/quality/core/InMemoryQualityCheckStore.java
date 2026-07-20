package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckId;
import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.data.quality.api.QualityCheckState;
import com.datausher.data.quality.api.QualityResult;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryQualityCheckStore implements QualityCheckStore {
    private final Map<QualityCheckId, QualityCheckRun> checks = new HashMap<>();
    private final Map<String, QualityCheckId> idempotencyIndex = new HashMap<>();
    private final Map<ExecutionRequestId, QualityCheckId> executionIndex = new HashMap<>();
    private final Map<QualityCheckId, List<QualityResult>> results = new HashMap<>();

    @Override
    public synchronized QualityCheckCreateResult createOrFind(QualityCheckRun check) {
        QualityCheckId existingId = idempotencyIndex.get(check.idempotencyKey());
        if (existingId != null) {
            return new QualityCheckCreateResult(checks.get(existingId), false);
        }
        if (checks.putIfAbsent(check.checkId(), check) != null) {
            throw new IllegalStateException(
                    "quality check already exists: " + check.checkId().value());
        }
        idempotencyIndex.put(check.idempotencyKey(), check.checkId());
        return new QualityCheckCreateResult(check, true);
    }

    @Override
    public synchronized void update(
            QualityCheckRun expected,
            QualityCheckRun replacement,
            List<QualityResult> replacementResults
    ) {
        if (!expected.checkId().equals(replacement.checkId())) {
            throw new IllegalArgumentException("quality check IDs must match");
        }
        if (!checks.replace(expected.checkId(), expected, replacement)) {
            QualityCheckRun actual = checks.get(expected.checkId());
            if (actual != null) {
                throw new RevisionConflictException(
                        "quality-check", expected.checkId().value(),
                        expected.revision(), actual.revision());
            }
            throw new IllegalStateException("quality check no longer exists");
        }
        replacement.executionRequestId().ifPresent(
                requestId -> executionIndex.put(requestId, replacement.checkId()));
        if (!replacementResults.isEmpty()) {
            results.put(replacement.checkId(), List.copyOf(replacementResults));
        }
    }

    @Override
    public synchronized Optional<QualityCheckRun> find(QualityCheckId checkId) {
        return Optional.ofNullable(checks.get(checkId));
    }

    @Override
    public synchronized Optional<QualityCheckRun> findByExecutionRequest(
            ExecutionRequestId requestId
    ) {
        return Optional.ofNullable(executionIndex.get(requestId)).map(checks::get);
    }

    @Override
    public synchronized List<QualityResult> listResults(QualityCheckId checkId) {
        return List.copyOf(results.getOrDefault(checkId, List.of()));
    }

    @Override
    public synchronized List<QualityCheckRun> findPending(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        return checks.values().stream()
                .filter(check -> check.state() == QualityCheckState.PENDING)
                .sorted(Comparator.comparing(QualityCheckRun::createdAt)
                        .thenComparing(check -> check.checkId().value()))
                .limit(limit)
                .toList();
    }
}
