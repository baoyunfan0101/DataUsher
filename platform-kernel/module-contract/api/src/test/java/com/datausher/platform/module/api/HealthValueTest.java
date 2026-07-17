package com.datausher.platform.module.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HealthValueTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void moduleHealthRequiresExplicitStatusAndTimestamp() {
        assertThrows(NullPointerException.class, () ->
                new ModuleHealth("catalog", null, "", NOW, Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                new ModuleHealth("catalog", HealthStatus.UP, "", null, Map.of())
        );
    }

    @Test
    void platformHealthRequiresExplicitStatusAndTimestamp() {
        assertThrows(NullPointerException.class, () ->
                new PlatformHealth(null, NOW, List.of())
        );
        assertThrows(NullPointerException.class, () ->
                new PlatformHealth(HealthStatus.UP, null, List.of())
        );
        assertThrows(NullPointerException.class, () ->
                new PlatformHealth(HealthStatus.UP, NOW, null)
        );
    }
}
