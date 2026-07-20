package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ProfileMetric;
import com.datausher.execution.api.ExecutionWorkloadType;

import java.util.List;

public interface ProfileResultDecoder {
    ExecutionWorkloadType workloadType();

    List<ProfileMetric> decode(ProfileResultDecodingRequest request);
}
