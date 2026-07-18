package com.datausher.development.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.workflow.api.WorkflowId;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;

public record RequestScriptPublication(
        ScriptId scriptId,
        long scriptVersion,
        WorkflowId workflowId,
        long baseWorkflowVersion,
        String taskKey,
        ApprovalTemplateKey approvalTemplateKey,
        SubjectRef requestedBy,
        String idempotencyKey,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public RequestScriptPublication {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        taskKey = Objects.requireNonNull(taskKey, "taskKey must not be null")
                .trim().toLowerCase(Locale.ROOT);
        approvalTemplateKey = Objects.requireNonNull(
                approvalTemplateKey, "approvalTemplateKey must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (scriptVersion < 1 || baseWorkflowVersion < 1
                || !taskKey.matches("[a-z][a-z0-9.-]{0,126}") || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("publication request contains invalid values");
        }
    }
}
