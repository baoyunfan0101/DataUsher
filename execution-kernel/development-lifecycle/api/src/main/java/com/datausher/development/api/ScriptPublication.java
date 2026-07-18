package com.datausher.development.api;

import com.datausher.governance.approval.api.ApprovalRequestId;
import com.datausher.workflow.api.WorkflowId;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record ScriptPublication(
        ScriptPublicationId publicationId,
        ScriptId scriptId,
        long scriptVersion,
        WorkflowId workflowId,
        long baseWorkflowVersion,
        String taskKey,
        String idempotencyKey,
        ScriptPublicationState state,
        Optional<ApprovalRequestId> approvalRequestId,
        Optional<Long> publishedWorkflowVersion,
        Optional<String> conflictCode,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public ScriptPublication {
        publicationId = Objects.requireNonNull(publicationId, "publicationId must not be null");
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        taskKey = normalizeTaskKey(taskKey);
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        state = Objects.requireNonNull(state, "state must not be null");
        approvalRequestId = approvalRequestId == null ? Optional.empty() : approvalRequestId;
        publishedWorkflowVersion = publishedWorkflowVersion == null
                ? Optional.empty() : publishedWorkflowVersion;
        conflictCode = conflictCode == null ? Optional.empty()
                : conflictCode.map(value -> value.trim().toLowerCase(Locale.ROOT));
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (scriptVersion < 1 || baseWorkflowVersion < 1 || idempotencyKey.isEmpty() || revision < 1) {
            throw new IllegalArgumentException("publication contains invalid values");
        }
        if ((state == ScriptPublicationState.REQUESTED) == approvalRequestId.isPresent()) {
            throw new IllegalArgumentException("approvalRequestId is required after approval submission");
        }
        if ((state == ScriptPublicationState.PUBLISHED) != publishedWorkflowVersion.isPresent()) {
            throw new IllegalArgumentException("published state requires publishedWorkflowVersion");
        }
        if ((state == ScriptPublicationState.CONFLICTED) != conflictCode.isPresent()) {
            throw new IllegalArgumentException("conflicted state requires conflictCode");
        }
        if (conflictCode.filter(value -> !value.matches("[a-z][a-z0-9.-]{0,126}")).isPresent()) {
            throw new IllegalArgumentException("conflictCode contains unsupported characters");
        }
        if (publishedWorkflowVersion.filter(value -> value <= baseWorkflowVersion).isPresent()) {
            throw new IllegalArgumentException("publishedWorkflowVersion must follow baseWorkflowVersion");
        }
    }

    private static String normalizeTaskKey(String value) {
        String normalized = Objects.requireNonNull(value, "taskKey must not be null")
                .trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("taskKey contains unsupported characters");
        }
        return normalized;
    }
}
