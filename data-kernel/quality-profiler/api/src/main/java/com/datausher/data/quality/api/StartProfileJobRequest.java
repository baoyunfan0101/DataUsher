package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StartProfileJobRequest(
        DataTargetRef target,
        List<ProfileMetricSpec> metrics,
        DataExecutionPolicy executionPolicy,
        String idempotencyKey,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public StartProfileJobRequest {
        target = Objects.requireNonNull(target, "target must not be null");
        metrics = List.copyOf(metrics);
        executionPolicy = Objects.requireNonNull(
                executionPolicy, "executionPolicy must not be null");
        idempotencyKey = QualityValues.text(idempotencyKey, "idempotencyKey");
        attributes = QualityValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("metrics must not be empty");
        }
        if (new HashSet<>(metrics.stream().map(ProfileMetricSpec::key).toList()).size()
                != metrics.size()) {
            throw new IllegalArgumentException("profile metric keys must be unique");
        }
    }
}
