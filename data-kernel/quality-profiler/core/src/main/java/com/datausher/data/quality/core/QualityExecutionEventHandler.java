package com.datausher.data.quality.core;

import com.datausher.execution.api.ExecutionStateChangedEvent;

public interface QualityExecutionEventHandler {
    void handleExecutionStateChanged(ExecutionStateChangedEvent event);
}
