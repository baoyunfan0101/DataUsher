package com.datausher.development.core;

import com.datausher.development.api.DebugRun;

import java.util.Objects;

public record DebugRunTransitionResult(DebugRun debugRun, boolean changed) {
    public DebugRunTransitionResult {
        debugRun = Objects.requireNonNull(debugRun, "debugRun must not be null");
    }
}
