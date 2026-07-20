package com.datausher.data.quality.api;

import java.time.Instant;
import java.util.Objects;

public record QualityRuleVersion(
        QualityRuleId ruleId,
        long version,
        QualityRuleSpec specification,
        Instant createdAt,
        String createdBy
) {
    public QualityRuleVersion {
        ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        specification = Objects.requireNonNull(
                specification, "specification must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        createdBy = QualityValues.text(createdBy, "createdBy");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }
}
