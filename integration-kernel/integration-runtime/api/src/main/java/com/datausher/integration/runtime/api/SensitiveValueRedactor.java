package com.datausher.integration.runtime.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SensitiveValueRedactor {
    public static final String REDACTION = "[redacted]";

    private final List<String> sensitiveValues;

    private SensitiveValueRedactor(Collection<String> sensitiveValues) {
        Set<String> values = sensitiveValues == null ? Set.of() : new LinkedHashSet<>(sensitiveValues);
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("sensitive values must not be blank");
        }
        this.sensitiveValues = values.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public static SensitiveValueRedactor empty() {
        return new SensitiveValueRedactor(Set.of());
    }

    public static SensitiveValueRedactor of(Collection<String> sensitiveValues) {
        return new SensitiveValueRedactor(sensitiveValues);
    }

    public String redact(String value) {
        if (value == null || sensitiveValues.isEmpty()) {
            return value;
        }
        String redacted = value;
        for (String sensitiveValue : sensitiveValues) {
            redacted = redacted.replace(sensitiveValue, REDACTION);
        }
        return redacted;
    }

    public Map<String, String> redact(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> IntegrationIdentifiers.requireText(entry.getKey(), "redaction key"),
                        entry -> Objects.requireNonNull(redact(entry.getValue()),
                                "redacted value must not be null")
                ));
    }

    public Set<String> sensitiveValues() {
        return Set.copyOf(sensitiveValues);
    }
}
