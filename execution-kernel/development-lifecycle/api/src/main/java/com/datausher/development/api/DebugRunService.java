package com.datausher.development.api;

import java.util.Optional;

public interface DebugRunService {
    DebugRun start(StartDebugRunRequest request);

    Optional<DebugRun> findDebugRun(DebugRunId debugRunId);
}
