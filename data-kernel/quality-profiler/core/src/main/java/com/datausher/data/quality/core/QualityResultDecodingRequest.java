package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityCheckRun;
import com.datausher.data.quality.api.QualityRuleVersion;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;

import java.util.List;
import java.util.Objects;

public record QualityResultDecodingRequest(
        QualityCheckRun check,
        List<QualityRuleVersion> rules,
        ExecutionRequest executionRequest,
        RequestContext requestContext
) {
    public QualityResultDecodingRequest {
        check = Objects.requireNonNull(check, "check must not be null");
        rules = List.copyOf(rules);
        executionRequest = Objects.requireNonNull(
                executionRequest, "executionRequest must not be null");
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
