package com.datausher.data.quality.api;

import java.util.Map;
import java.util.Objects;

public record DataTargetRef(
        DataTargetType type,
        String targetId,
        Map<String, String> options
) {
    public DataTargetRef {
        type = Objects.requireNonNull(type, "type must not be null");
        targetId = QualityValues.text(targetId, "targetId");
        options = QualityValues.attributes(options);
    }
}
