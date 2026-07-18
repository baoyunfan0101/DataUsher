package com.datausher.development.api;

import com.datausher.governance.resource.api.ResourceRef;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ScriptDefinition(
        ScriptId scriptId,
        ResourceRef resourceRef,
        String displayName,
        ScriptLanguage language,
        long latestVersion,
        long revision,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        Map<String, String> attributes
) {
    public ScriptDefinition {
        scriptId = Objects.requireNonNull(scriptId, "scriptId must not be null");
        resourceRef = Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        language = Objects.requireNonNull(language, "language must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null").trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (!resourceRef.resourceType().equals("script")
                || displayName.isEmpty() || createdBy.isEmpty()
                || latestVersion < 0 || revision < 1) {
            throw new IllegalArgumentException("script definition contains invalid values");
        }
    }
}
