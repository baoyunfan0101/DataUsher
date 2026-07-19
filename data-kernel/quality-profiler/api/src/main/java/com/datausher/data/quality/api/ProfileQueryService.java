package com.datausher.data.quality.api;

import java.util.List;
import java.util.Optional;

public interface ProfileQueryService {
    Optional<ProfileJob> findJob(ProfileJobId jobId);

    List<ProfileMetric> listMetrics(ProfileJobId jobId);
}
