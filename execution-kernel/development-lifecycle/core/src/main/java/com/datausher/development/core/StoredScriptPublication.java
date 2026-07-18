package com.datausher.development.core;

import com.datausher.development.api.ScriptPublication;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.approval.api.ApprovalTemplateKey;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record StoredScriptPublication(
        ScriptPublication publication,
        ApprovalTemplateKey approvalTemplateKey,
        SubjectRef requestedBy,
        Map<String, String> approvalAttributes,
        RequestContext requestContext
) {
    public StoredScriptPublication {
        publication = Objects.requireNonNull(publication, "publication must not be null");
        approvalTemplateKey = Objects.requireNonNull(
                approvalTemplateKey, "approvalTemplateKey must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        approvalAttributes = approvalAttributes == null ? Map.of() : Map.copyOf(approvalAttributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
