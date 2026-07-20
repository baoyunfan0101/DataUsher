package com.datausher.ai.guardrails.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AiGuardrailRequest(
        Set<SubjectRef> subjects,
        String action,
        ResourceRef resource,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public AiGuardrailRequest {
        subjects = subjects == null ? Set.of() : Set.copyOf(subjects);
        action = AiGuardrailValues.id(action, "action");
        resource = Objects.requireNonNull(resource, "resource must not be null");
        attributes = AiGuardrailValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException("subjects must not be empty");
        }
    }
}
