package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.AiGuardrailDecisionCode;
import com.datausher.ai.guardrails.api.AiGuardrailFinding;
import com.datausher.ai.guardrails.api.AiGuardrailFindingType;
import com.datausher.ai.guardrails.api.AiGuardrailRequest;
import com.datausher.ai.guardrails.api.AiGuardrailReview;
import com.datausher.ai.guardrails.api.AiGuardrailService;
import com.datausher.ai.guardrails.api.AiGuardrailSeverity;
import com.datausher.governance.access.api.AccessDecision;
import com.datausher.governance.access.api.AccessDecisionService;
import com.datausher.governance.access.api.AccessRequest;
import com.datausher.platform.shared.time.Clock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultAiGuardrailService implements AiGuardrailService {
    private final AccessDecisionService accessDecisions;
    private final Clock clock;

    public DefaultAiGuardrailService(AccessDecisionService accessDecisions, Clock clock) {
        this.accessDecisions = Objects.requireNonNull(
                accessDecisions, "accessDecisions must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AiGuardrailReview review(AiGuardrailRequest request) {
        AccessDecision decision = accessDecisions.decide(new AccessRequest(
                request.subjects(), request.action(), request.resource(),
                request.requestContext(), request.attributes()));
        if (decision.allowed()) {
            return new AiGuardrailReview(
                    true, AiGuardrailDecisionCode.ALLOWED, List.of(), Map.of(), clock.now());
        }
        return new AiGuardrailReview(false, AiGuardrailDecisionCode.DENIED_BY_PERMISSION,
                List.of(new AiGuardrailFinding(
                        AiGuardrailFindingType.PERMISSION, AiGuardrailSeverity.BLOCKING,
                        decision.reason(), Map.of("code", decision.code().name()))),
                Map.of(), clock.now());
    }
}
