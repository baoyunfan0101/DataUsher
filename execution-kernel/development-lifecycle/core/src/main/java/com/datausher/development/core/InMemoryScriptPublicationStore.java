package com.datausher.development.core;

import com.datausher.development.api.ScriptPublication;
import com.datausher.development.api.ScriptPublicationId;
import com.datausher.development.api.ScriptPublicationState;
import com.datausher.governance.approval.api.ApprovalRequestId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryScriptPublicationStore implements ScriptPublicationStore {
    private final Map<ScriptPublicationId, StoredScriptPublication> publications = new HashMap<>();
    private final Map<String, ScriptPublicationId> idempotencyIndex = new HashMap<>();

    @Override
    public synchronized ScriptPublicationCreateResult createOrFind(StoredScriptPublication publication) {
        ScriptPublication proposed = publication.publication();
        ScriptPublicationId existingId = idempotencyIndex.get(proposed.idempotencyKey());
        if (existingId != null) {
            return new ScriptPublicationCreateResult(publications.get(existingId), false);
        }
        if (publications.putIfAbsent(proposed.publicationId(), publication) != null) {
            throw new IllegalStateException("publication already exists: " + proposed.publicationId());
        }
        idempotencyIndex.put(proposed.idempotencyKey(), proposed.publicationId());
        return new ScriptPublicationCreateResult(publication, true);
    }

    @Override
    public synchronized ScriptPublicationTransitionResult attachApproval(
            ScriptPublication expected,
            ApprovalRequestId approvalRequestId,
            Instant updatedAt
    ) {
        StoredScriptPublication stored = require(expected.publicationId());
        ScriptPublication current = stored.publication();
        if (!current.equals(expected)) {
            if (current.approvalRequestId().filter(approvalRequestId::equals).isPresent()) {
                return new ScriptPublicationTransitionResult(current, false);
            }
            throw new IllegalStateException("publication changed concurrently: " + current.publicationId());
        }
        ScriptPublication pending = copy(
                current, ScriptPublicationState.APPROVAL_PENDING,
                Optional.of(approvalRequestId), Optional.empty(), Optional.empty(), updatedAt);
        publications.put(current.publicationId(), withPublication(stored, pending));
        return new ScriptPublicationTransitionResult(pending, true);
    }

    @Override
    public synchronized ScriptPublicationTransitionResult complete(
            ScriptPublication expected,
            ScriptPublicationState state,
            Optional<Long> publishedWorkflowVersion,
            Optional<String> conflictCode,
            Instant updatedAt
    ) {
        if (!state.terminal()) {
            throw new IllegalArgumentException("completion state must be terminal");
        }
        StoredScriptPublication stored = require(expected.publicationId());
        ScriptPublication current = stored.publication();
        if (!current.equals(expected)) {
            if (current.state() == state
                    && current.publishedWorkflowVersion().equals(publishedWorkflowVersion)
                    && current.conflictCode().equals(conflictCode)) {
                return new ScriptPublicationTransitionResult(current, false);
            }
            throw new IllegalStateException("publication changed concurrently: " + current.publicationId());
        }
        ScriptPublication completed = copy(
                current, state, current.approvalRequestId(),
                publishedWorkflowVersion, conflictCode, updatedAt);
        publications.put(current.publicationId(), withPublication(stored, completed));
        return new ScriptPublicationTransitionResult(completed, true);
    }

    @Override
    public synchronized Optional<StoredScriptPublication> find(ScriptPublicationId publicationId) {
        return Optional.ofNullable(publications.get(publicationId));
    }

    @Override
    public synchronized Optional<StoredScriptPublication> findByIdempotencyKey(String idempotencyKey) {
        ScriptPublicationId publicationId = idempotencyIndex.get(idempotencyKey);
        return publicationId == null ? Optional.empty() : Optional.of(publications.get(publicationId));
    }

    private StoredScriptPublication require(ScriptPublicationId publicationId) {
        StoredScriptPublication publication = publications.get(publicationId);
        if (publication == null) {
            throw new IllegalArgumentException("publication does not exist: " + publicationId);
        }
        return publication;
    }

    private static StoredScriptPublication withPublication(
            StoredScriptPublication stored,
            ScriptPublication publication
    ) {
        return new StoredScriptPublication(
                publication, stored.approvalTemplateKey(), stored.requestedBy(),
                stored.approvalAttributes(), stored.requestContext());
    }

    private static ScriptPublication copy(
            ScriptPublication current,
            ScriptPublicationState state,
            Optional<ApprovalRequestId> approvalRequestId,
            Optional<Long> publishedWorkflowVersion,
            Optional<String> conflictCode,
            Instant updatedAt
    ) {
        return new ScriptPublication(
                current.publicationId(), current.scriptId(), current.scriptVersion(),
                current.workflowId(), current.baseWorkflowVersion(), current.taskKey(),
                current.idempotencyKey(), state, approvalRequestId, publishedWorkflowVersion,
                conflictCode, current.createdAt(), updatedAt, current.revision() + 1);
    }
}
