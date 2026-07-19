package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record CreateQualityRuleVersionRequest(
        QualityRuleId ruleId,
        long expectedRevision,
        QualityRuleSpec specification,
        RequestContext requestContext
) {
    public CreateQualityRuleVersionRequest {
        ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        specification = Objects.requireNonNull(
                specification, "specification must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
