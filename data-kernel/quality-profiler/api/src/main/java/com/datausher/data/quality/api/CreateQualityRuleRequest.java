package com.datausher.data.quality.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record CreateQualityRuleRequest(
        QualityRuleId ruleId,
        QualityRuleSpec specification,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateQualityRuleRequest {
        ruleId = Objects.requireNonNull(ruleId, "ruleId must not be null");
        specification = Objects.requireNonNull(
                specification, "specification must not be null");
        attributes = QualityValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
