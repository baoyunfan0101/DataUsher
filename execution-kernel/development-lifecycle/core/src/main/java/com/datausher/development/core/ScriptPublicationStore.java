package com.datausher.development.core;

import com.datausher.development.api.ScriptPublication;
import com.datausher.development.api.ScriptPublicationId;
import com.datausher.development.api.ScriptPublicationState;
import com.datausher.governance.approval.api.ApprovalRequestId;

import java.time.Instant;
import java.util.Optional;

public interface ScriptPublicationStore {
    ScriptPublicationCreateResult createOrFind(StoredScriptPublication publication);

    ScriptPublicationTransitionResult attachApproval(
            ScriptPublication expected,
            ApprovalRequestId approvalRequestId,
            Instant updatedAt
    );

    ScriptPublicationTransitionResult complete(
            ScriptPublication expected,
            ScriptPublicationState state,
            Optional<Long> publishedWorkflowVersion,
            Optional<String> conflictCode,
            Instant updatedAt
    );

    Optional<StoredScriptPublication> find(ScriptPublicationId publicationId);

    Optional<StoredScriptPublication> findByIdempotencyKey(String idempotencyKey);
}
