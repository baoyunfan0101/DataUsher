package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionValue;

import java.util.Map;
import java.util.Objects;

public record QualityRuleSpec(
        String displayName,
        DataTargetRef target,
        QualityRuleType type,
        Map<String, ExecutionValue> parameters,
        QualitySeverity severity,
        Map<String, String> attributes
) {
    public QualityRuleSpec {
        displayName = QualityValues.text(displayName, "displayName");
        target = Objects.requireNonNull(target, "target must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        severity = Objects.requireNonNull(severity, "severity must not be null");
        attributes = QualityValues.attributes(attributes);
    }
}
