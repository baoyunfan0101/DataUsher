package com.datausher.data.quality.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProfileMetricSpec(
        ProfileMetricType type,
        Optional<String> field,
        Map<String, String> options
) {
    public ProfileMetricSpec {
        type = Objects.requireNonNull(type, "type must not be null");
        field = QualityValues.optional(field);
        options = QualityValues.attributes(options);
    }

    public String key() {
        return type.value() + ":" + field.orElse("");
    }
}
