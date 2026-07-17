package com.datausher.governance.resource.api;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record ResourceTypeDefinition(
        String resourceType,
        String ownerModule,
        String displayName,
        Set<String> actions
) {
    public ResourceTypeDefinition {
        resourceType = new ResourceRef(resourceType, "validation", ResourceScope.global()).resourceType();
        ownerModule = normalizeOwner(ownerModule);
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        actions = actions == null ? Set.of() : normalizeActions(actions);
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
    }

    private static String normalizeOwner(String value) {
        String normalized = Objects.requireNonNull(value, "ownerModule must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9-]{0,126}")) {
            throw new IllegalArgumentException("ownerModule must match [a-z][a-z0-9-]{0,126}");
        }
        return normalized;
    }

    public boolean supports(String action) {
        return actions.contains(normalizeAction(action));
    }

    private static Set<String> normalizeActions(Set<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (!normalized.add(normalizeAction(value))) {
                throw new IllegalArgumentException("duplicate action: " + value);
            }
        }
        return Set.copyOf(normalized);
    }

    private static String normalizeAction(String value) {
        String normalized = Objects.requireNonNull(value, "action must not be null").trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("action must match [a-z][a-z0-9.-]{0,126}");
        }
        return normalized;
    }
}
