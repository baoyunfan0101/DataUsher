package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ApprovalStep(
        String stepKey,
        String displayName,
        Set<SubjectRef> eligibleApprovers,
        int requiredApprovals,
        Set<String> dependsOn,
        ApprovalStepStatus status,
        List<ApprovalStepDecision> decisions
) {
    public ApprovalStep {
        stepKey = Objects.requireNonNull(stepKey, "stepKey must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        eligibleApprovers = Set.copyOf(Objects.requireNonNull(
                eligibleApprovers, "eligibleApprovers must not be null"));
        dependsOn = Set.copyOf(Objects.requireNonNull(dependsOn, "dependsOn must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions must not be null"));
        if (eligibleApprovers.isEmpty() || requiredApprovals < 1 || requiredApprovals > eligibleApprovers.size()) {
            throw new IllegalArgumentException("requiredApprovals must be covered by eligibleApprovers");
        }
    }
}
