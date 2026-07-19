package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckId;
import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.platform.shared.context.RequestContext;

import java.util.List;

public interface QualityCheckWorker {
    List<QualityCheckRun> findPending(int limit);

    QualityCheckRun dispatch(QualityCheckId checkId, RequestContext requestContext);
}
