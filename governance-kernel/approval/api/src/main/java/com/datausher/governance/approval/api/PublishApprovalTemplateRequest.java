package com.datausher.governance.approval.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PublishApprovalTemplateRequest(
        ApprovalTemplateKey templateKey,
        String displayName,
        ApprovalPurpose purpose,
        List<ApprovalStepDefinition> steps,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public PublishApprovalTemplateRequest {
        templateKey = Objects.requireNonNull(templateKey, "templateKey must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps must not be null"));
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty() || steps.isEmpty()) {
            throw new IllegalArgumentException("displayName and steps must not be empty");
        }
        if (new HashSet<>(steps.stream().map(ApprovalStepDefinition::stepKey).toList()).size() != steps.size()) {
            throw new IllegalArgumentException("step keys must be unique");
        }
        validateDependencies(steps);
    }

    private static void validateDependencies(List<ApprovalStepDefinition> steps) {
        var keys = steps.stream()
                .map(ApprovalStepDefinition::stepKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (ApprovalStepDefinition step : steps) {
            if (!keys.containsAll(step.dependsOn())) {
                throw new IllegalArgumentException("approval step dependency does not exist: " + step.stepKey());
            }
        }
        var resolved = new HashSet<String>();
        boolean changed;
        do {
            changed = false;
            for (ApprovalStepDefinition step : steps) {
                if (!resolved.contains(step.stepKey()) && resolved.containsAll(step.dependsOn())) {
                    resolved.add(step.stepKey());
                    changed = true;
                }
            }
        } while (changed);
        if (resolved.size() != steps.size()) {
            throw new IllegalArgumentException("approval step dependencies must be acyclic");
        }
    }
}
