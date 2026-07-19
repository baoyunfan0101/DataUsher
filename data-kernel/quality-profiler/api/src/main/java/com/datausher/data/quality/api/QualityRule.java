package com.datausher.data.quality.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record QualityRule(
        QualityRuleId ruleId,
        long latestVersion,
        QualityRuleStatus status,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public QualityRule {
        ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = QualityValues.attributes(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (latestVersion < 1 || revision < 1 || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("quality rule contains invalid audit values");
        }
    }
}
