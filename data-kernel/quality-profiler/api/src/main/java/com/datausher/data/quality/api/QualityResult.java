package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record QualityResult(
        QualityCheckId checkId,
        QualityRuleRef rule,
        QualityOutcome outcome,
        QualitySeverity severity,
        Optional<ExecutionValue> observedValue,
        String message,
        Map<String, String> attributes,
        Instant observedAt
) {
    public QualityResult {
        checkId = Objects.requireNonNull(checkId, "checkId must not be null");
        rule = Objects.requireNonNull(rule, "rule must not be null");
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        severity = Objects.requireNonNull(severity, "severity must not be null");
        observedValue = observedValue == null ? Optional.empty() : observedValue;
        message = message == null ? "" : message.trim();
        attributes = QualityValues.attributes(attributes);
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
    }
}
