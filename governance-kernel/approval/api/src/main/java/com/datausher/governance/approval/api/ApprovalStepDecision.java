package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;

import java.time.Instant;
import java.util.Objects;

public record ApprovalStepDecision(
        SubjectRef approver,
        ApprovalDecisionType decision,
        String comment,
        Instant decidedAt
) {
    public ApprovalStepDecision {
        approver = Objects.requireNonNull(approver, "approver must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null");
        comment = comment == null ? "" : comment.trim();
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    }
}
