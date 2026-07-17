package com.datausher.platform.module.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ModuleHealth(
        String moduleName,
        HealthStatus status,
        String message,
        Instant checkedAt,
        Map<String, String> details
) {
    public ModuleHealth {
        moduleName = ModuleIdentifiers.normalizeModuleName(moduleName);
        status = Objects.requireNonNull(status, "status must not be null");
        message = message == null ? "" : message.trim();
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
