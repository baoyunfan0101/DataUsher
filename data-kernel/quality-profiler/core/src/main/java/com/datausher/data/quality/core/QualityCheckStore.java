package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckId;
import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.data.quality.api.QualityResult;
import com.datausher.execution.api.ExecutionRequestId;

import java.util.List;
import java.util.Optional;

public interface QualityCheckStore {
    QualityCheckCreateResult createOrFind(QualityCheckRun check);

    void update(
            QualityCheckRun expected,
            QualityCheckRun replacement,
            List<QualityResult> results
    );

    Optional<QualityCheckRun> find(QualityCheckId checkId);

    Optional<QualityCheckRun> findByExecutionRequest(ExecutionRequestId requestId);

    List<QualityResult> listResults(QualityCheckId checkId);

    List<QualityCheckRun> findPending(int limit);
}
