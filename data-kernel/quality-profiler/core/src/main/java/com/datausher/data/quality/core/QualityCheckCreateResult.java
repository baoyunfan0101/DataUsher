package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckRun;

import java.util.Objects;

public record QualityCheckCreateResult(QualityCheckRun check, boolean created) {
    public QualityCheckCreateResult {
        check = Objects.requireNonNull(check, "check must not be null");
    }
}
