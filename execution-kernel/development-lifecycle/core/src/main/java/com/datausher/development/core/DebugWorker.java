package com.datausher.development.core;

import com.datausher.development.api.DebugRun;
import com.datausher.development.api.DebugRunId;
import com.datausher.platform.shared.context.RequestContext;

public interface DebugWorker {
    DebugRun dispatch(DebugRunId debugRunId, RequestContext requestContext);
}
