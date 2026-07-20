package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileJob;
import com.datausher.data.quality.api.ProfileJobId;
import com.datausher.data.quality.api.ProfileMetric;
import com.datausher.execution.api.ExecutionRequestId;

import java.util.List;
import java.util.Optional;

public interface ProfileStore {
    ProfileJobCreateResult createOrFind(ProfileJob job);

    void update(ProfileJob expected, ProfileJob replacement, List<ProfileMetric> metrics);

    Optional<ProfileJob> find(ProfileJobId jobId);

    Optional<ProfileJob> findByExecutionRequest(ExecutionRequestId requestId);

    List<ProfileMetric> listMetrics(ProfileJobId jobId);

    List<ProfileJob> findPending(int limit);
}
