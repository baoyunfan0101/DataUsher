package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionRequestId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record QualityCheckRun(
        QualityCheckId checkId,
        List<QualityRuleRef> rules,
        DataExecutionPolicy executionPolicy,
        String idempotencyKey,
        QualityCheckState state,
        Optional<ExecutionRequestId> executionRequestId,
        Optional<String> failureCode,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        Optional<Instant> finishedAt,
        long revision
) {
    public QualityCheckRun {
        checkId = Objects.requireNonNull(checkId, "checkId must not be null");
        rules = List.copyOf(rules);
        executionPolicy = Objects.requireNonNull(
                executionPolicy, "executionPolicy must not be null");
        idempotencyKey = QualityValues.text(idempotencyKey, "idempotencyKey");
        state = Objects.requireNonNull(state, "state must not be null");
        executionRequestId = executionRequestId == null ? Optional.empty() : executionRequestId;
        failureCode = QualityValues.optional(failureCode);
        attributes = QualityValues.attributes(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        finishedAt = finishedAt == null ? Optional.empty() : finishedAt;
        if (rules.isEmpty() || revision < 1 || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("quality check contains invalid values");
        }
        if (state == QualityCheckState.PENDING && executionRequestId.isPresent()) {
            throw new IllegalArgumentException("pending quality check must not have an execution request");
        }
        if (state != QualityCheckState.PENDING && executionRequestId.isEmpty()) {
            throw new IllegalArgumentException("started quality check requires an execution request");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
    }
}
