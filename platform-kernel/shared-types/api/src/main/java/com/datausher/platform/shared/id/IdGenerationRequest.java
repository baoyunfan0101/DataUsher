package com.datausher.platform.shared.id;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IdGenerationRequest(
        String domain,
        String entityType,
        Map<String, String> attributes
) {
    public static final String GLOBAL_DOMAIN = "global";
    public static final String DEFAULT_ENTITY_TYPE = "default";

    public IdGenerationRequest {
        domain = normalize(domain, "domain");
        entityType = normalize(entityType, "entityType");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static IdGenerationRequest global() {
        return new IdGenerationRequest(GLOBAL_DOMAIN, DEFAULT_ENTITY_TYPE, Map.of());
    }

    public static IdGenerationRequest of(String domain, String entityType) {
        return new IdGenerationRequest(domain, entityType, Map.of());
    }

    public IdGenerationRequest withAttribute(String name, String value) {
        String normalizedName = normalize(name, "name");
        String normalizedValue = Objects.requireNonNull(value, "value must not be null").trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        Map<String, String> next = new LinkedHashMap<>(attributes);
        next.put(normalizedName, normalizedValue);
        return new IdGenerationRequest(domain, entityType, next);
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
