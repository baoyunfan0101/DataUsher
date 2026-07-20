package com.datausher.ai.context.api;

import com.datausher.governance.resource.api.ResourceRef;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AiContextSourceRef(
        AiContextSourceType type,
        String externalId,
        Optional<ResourceRef> resource,
        Map<String, String> attributes
) {
    public AiContextSourceRef {
        type = Objects.requireNonNull(type, "type must not be null");
        externalId = AiContextValues.text(externalId, "externalId");
        resource = resource == null ? Optional.empty() : resource;
        attributes = AiContextValues.attributes(attributes);
    }
}
