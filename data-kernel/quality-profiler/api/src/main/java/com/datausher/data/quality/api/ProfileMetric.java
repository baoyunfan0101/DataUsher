package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProfileMetric(
        ProfileJobId jobId,
        ProfileMetricType type,
        Optional<String> field,
        ExecutionValue value,
        Map<String, String> attributes,
        Instant observedAt
) {
    public ProfileMetric {
        jobId = Objects.requireNonNull(jobId, "jobId must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        field = QualityValues.optional(field);
        value = Objects.requireNonNull(value, "value must not be null");
        attributes = QualityValues.attributes(attributes);
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
    }

    public String key() {
        return type.value() + ":" + field.orElse("");
    }
}
