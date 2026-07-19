package com.datausher.data.quality.core;

import com.datausher.execution.api.ExecutionStateChangedEvent;

public interface ProfileExecutionEventHandler {
    void handleExecutionStateChanged(ExecutionStateChangedEvent event);
}
