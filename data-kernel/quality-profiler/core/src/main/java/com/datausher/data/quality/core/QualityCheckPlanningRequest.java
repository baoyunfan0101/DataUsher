package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.data.quality.api.QualityRuleVersion;

import java.util.List;
import java.util.Objects;

public record QualityCheckPlanningRequest(
        QualityCheckRun check,
        List<QualityRuleVersion> rules
) {
    public QualityCheckPlanningRequest {
        check = Objects.requireNonNull(check, "check must not be null");
        rules = List.copyOf(rules);
    }
}
