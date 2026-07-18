package com.datausher.workflow.core;

import com.datausher.workflow.api.WorkflowPublication;

import java.util.Objects;

public record WorkflowPublicationCreateResult(WorkflowPublication publication, boolean created) {
    public WorkflowPublicationCreateResult {
        publication = Objects.requireNonNull(publication, "publication must not be null");
    }
}
