package com.datausher.platform.module.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PlatformHealth(HealthStatus status, Instant checkedAt, List<ModuleHealth> modules) {
    public PlatformHealth {
        status = Objects.requireNonNull(status, "status must not be null");
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        modules = List.copyOf(Objects.requireNonNull(modules, "modules must not be null"));
    }
}
