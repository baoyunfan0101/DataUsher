package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileJob;
import com.datausher.data.quality.api.ProfileJobId;
import com.datausher.data.quality.api.ProfileJobState;
import com.datausher.data.quality.api.ProfileMetric;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryProfileStore implements ProfileStore {
    private final Map<ProfileJobId, ProfileJob> jobs = new HashMap<>();
    private final Map<String, ProfileJobId> idempotencyIndex = new HashMap<>();
    private final Map<ExecutionRequestId, ProfileJobId> executionIndex = new HashMap<>();
    private final Map<ProfileJobId, List<ProfileMetric>> metrics = new HashMap<>();

    @Override
    public synchronized ProfileJobCreateResult createOrFind(ProfileJob job) {
        ProfileJobId existingId = idempotencyIndex.get(job.idempotencyKey());
        if (existingId != null) {
            return new ProfileJobCreateResult(jobs.get(existingId), false);
        }
        if (jobs.putIfAbsent(job.jobId(), job) != null) {
            throw new IllegalStateException("profile job already exists: " + job.jobId().value());
        }
        idempotencyIndex.put(job.idempotencyKey(), job.jobId());
        return new ProfileJobCreateResult(job, true);
    }

    @Override
    public synchronized void update(
            ProfileJob expected,
            ProfileJob replacement,
            List<ProfileMetric> replacementMetrics
    ) {
        if (!expected.jobId().equals(replacement.jobId())) {
            throw new IllegalArgumentException("profile job IDs must match");
        }
        if (!jobs.replace(expected.jobId(), expected, replacement)) {
            ProfileJob actual = jobs.get(expected.jobId());
            if (actual != null) {
                throw new RevisionConflictException(
                        "profile-job", expected.jobId().value(),
                        expected.revision(), actual.revision());
            }
            throw new IllegalStateException("profile job no longer exists");
        }
        replacement.executionRequestId().ifPresent(
                requestId -> executionIndex.put(requestId, replacement.jobId()));
        if (!replacementMetrics.isEmpty()) {
            metrics.put(replacement.jobId(), List.copyOf(replacementMetrics));
        }
    }

    @Override
    public synchronized Optional<ProfileJob> find(ProfileJobId jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public synchronized Optional<ProfileJob> findByExecutionRequest(ExecutionRequestId requestId) {
        return Optional.ofNullable(executionIndex.get(requestId)).map(jobs::get);
    }

    @Override
    public synchronized List<ProfileMetric> listMetrics(ProfileJobId jobId) {
        return List.copyOf(metrics.getOrDefault(jobId, List.of()));
    }

    @Override
    public synchronized List<ProfileJob> findPending(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        return jobs.values().stream()
                .filter(job -> job.state() == ProfileJobState.PENDING)
                .sorted(Comparator.comparing(ProfileJob::createdAt)
                        .thenComparing(job -> job.jobId().value()))
                .limit(limit)
                .toList();
    }
}
