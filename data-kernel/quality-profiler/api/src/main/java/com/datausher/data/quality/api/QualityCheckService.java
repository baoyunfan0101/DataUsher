package com.datausher.data.quality.api;

import java.util.List;
import java.util.Optional;

public interface QualityCheckService {
    QualityCheckRun start(StartQualityCheckRequest request);

    QualityCheckRun cancel(CancelQualityCheckRequest request);

    Optional<QualityCheckRun> findCheck(QualityCheckId checkId);

    List<QualityResult> listResults(QualityCheckId checkId);
}
