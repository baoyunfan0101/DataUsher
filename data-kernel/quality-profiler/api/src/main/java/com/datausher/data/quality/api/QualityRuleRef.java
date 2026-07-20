package com.datausher.data.quality.api;

import java.util.Objects;

public record QualityRuleRef(QualityRuleId ruleId, long version) {
    public QualityRuleRef {
        ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }
}
