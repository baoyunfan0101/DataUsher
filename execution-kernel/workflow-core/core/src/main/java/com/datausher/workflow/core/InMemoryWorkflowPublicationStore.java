package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowPublication;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWorkflowPublicationStore implements WorkflowPublicationStore {
    private final Map<String, WorkflowPublication> publications = new HashMap<>();
    private final Map<String, String> idempotencyIndex = new HashMap<>();

    @Override
    public synchronized WorkflowPublicationCreateResult createOrFind(WorkflowPublication publication) {
        String existingKey = idempotencyIndex.get(publication.idempotencyKey());
        if (existingKey != null) {
            return new WorkflowPublicationCreateResult(publications.get(existingKey), false);
        }
        String key = key(publication.workflowId(), publication.version());
        WorkflowPublication existing = publications.get(key);
        if (existing != null) {
            return new WorkflowPublicationCreateResult(existing, false);
        }
        publications.put(key, publication);
        idempotencyIndex.put(publication.idempotencyKey(), key);
        return new WorkflowPublicationCreateResult(publication, true);
    }

    @Override
    public synchronized Optional<WorkflowPublication> find(WorkflowId workflowId, long version) {
        return Optional.ofNullable(publications.get(key(workflowId, version)));
    }

    private static String key(WorkflowId workflowId, long version) {
        return workflowId.value() + "@" + version;
    }
}
