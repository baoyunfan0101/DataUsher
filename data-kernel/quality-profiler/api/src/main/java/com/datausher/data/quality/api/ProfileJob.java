package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionRequestId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProfileJob(
        ProfileJobId jobId,
        DataTargetRef target,
        List<ProfileMetricSpec> metrics,
        DataExecutionPolicy executionPolicy,
        String idempotencyKey,
        ProfileJobState state,
        Optional<ExecutionRequestId> executionRequestId,
        Optional<String> failureCode,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        Optional<Instant> finishedAt,
        long revision
) {
    public ProfileJob {
        jobId = Objects.requireNonNull(jobId, "jobId must not be null");
        target = Objects.requireNonNull(target, "target must not be null");
        metrics = List.copyOf(metrics);
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
        if (metrics.isEmpty() || revision < 1 || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("profile job contains invalid values");
        }
        if (state == ProfileJobState.PENDING && executionRequestId.isPresent()) {
            throw new IllegalArgumentException("pending profile job must not have an execution request");
        }
        if (state != ProfileJobState.PENDING && executionRequestId.isEmpty()) {
            throw new IllegalArgumentException("started profile job requires an execution request");
        }
        if (state.terminal() != finishedAt.isPresent()) {
            throw new IllegalArgumentException("finishedAt must be present exactly for terminal states");
        }
    }
}
