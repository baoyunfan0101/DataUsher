package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StartQualityCheckRequest(
        List<QualityRuleRef> rules,
        DataExecutionPolicy executionPolicy,
        String idempotencyKey,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public StartQualityCheckRequest {
        rules = List.copyOf(rules);
        executionPolicy = Objects.requireNonNull(
                executionPolicy, "executionPolicy must not be null");
        idempotencyKey = QualityValues.text(idempotencyKey, "idempotencyKey");
        attributes = QualityValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }
        if (new HashSet<>(rules).size() != rules.size()) {
            throw new IllegalArgumentException("quality rule references must be unique");
        }
    }
}
