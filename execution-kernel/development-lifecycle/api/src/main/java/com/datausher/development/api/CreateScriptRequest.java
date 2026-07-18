package com.datausher.development.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record CreateScriptRequest(
        ScriptId scriptId,
        ResourceRef resourceRef,
        String displayName,
        ScriptLanguage language,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateScriptRequest {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        language = Objects.requireNonNull(language, "language must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
