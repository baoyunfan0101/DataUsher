package com.datausher.development.core;

import com.datausher.development.api.DebugRun;

import java.util.Objects;

public record DebugRunCreateResult(DebugRun debugRun, boolean created) {
    public DebugRunCreateResult {
        debugRun = Objects.requireNonNull(debugRun, "debugRun must not be null");
    }
}
