package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityResult;
import com.datausher.execution.api.ExecutionWorkloadType;

import java.util.List;

public interface QualityResultDecoder {
    ExecutionWorkloadType workloadType();

    List<QualityResult> decode(QualityResultDecodingRequest request);
}
