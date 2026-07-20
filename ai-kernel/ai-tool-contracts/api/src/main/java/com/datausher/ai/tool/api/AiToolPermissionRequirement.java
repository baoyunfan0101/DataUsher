package com.datausher.ai.tool.api;

import com.datausher.governance.resource.api.ResourceRef;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AiToolPermissionRequirement(
        ResourceRef resource,
        Set<String> actions,
        Map<String, String> attributes
) {
    public AiToolPermissionRequirement {
        resource = Objects.requireNonNull(resource, "resource must not be null");
        actions = actions == null ? Set.of() : Set.copyOf(actions.stream()
                .map(action -> AiToolValues.id(action, "action")).toList());
        attributes = AiToolValues.attributes(attributes);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
    }
}
