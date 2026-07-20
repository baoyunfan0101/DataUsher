package com.datausher.ai.runtime.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record StartAiConversationRequest(
        String title,
        Optional<SubjectRef> owner,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public StartAiConversationRequest {
        title = AiRuntimeValues.text(title, "title");
        owner = owner == null ? Optional.empty() : owner;
        attributes = AiRuntimeValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
    }
}
