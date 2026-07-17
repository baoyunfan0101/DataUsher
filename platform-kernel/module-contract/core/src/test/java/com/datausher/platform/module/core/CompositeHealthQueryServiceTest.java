package com.datausher.platform.module.core;

import com.datausher.platform.module.api.HealthStatus;
import com.datausher.platform.module.api.ModuleHealth;
import com.datausher.platform.module.api.ModuleHealthContributor;
import com.datausher.platform.module.api.PlatformHealth;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeHealthQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");
    private static final Clock CLOCK = new FixedClock(NOW);

    @Test
    void reportsUnknownWhenNoHealthContributorsExist() {
        CompositeHealthQueryService service = new CompositeHealthQueryService(CLOCK, List.of());

        PlatformHealth health = service.platformHealth();

        assertEquals(HealthStatus.UNKNOWN, health.status());
        assertEquals(NOW, health.checkedAt());
        assertEquals(List.of(), health.modules());
    }

    @Test
    void isolatesContributorFailureAndPreservesModuleIdentity() {
        ModuleHealthContributor healthy = contributor("catalog", () ->
                health("catalog", HealthStatus.UP)
        );
        ModuleHealthContributor failing = contributor("workflow", () -> {
            throw new IllegalStateException("backend unavailable");
        });
        CompositeHealthQueryService service = new CompositeHealthQueryService(CLOCK, List.of(failing, healthy));

        PlatformHealth health = service.platformHealth();

        assertEquals(HealthStatus.UNKNOWN, health.status());
        assertEquals(List.of("catalog", "workflow"), health.modules().stream().map(ModuleHealth::moduleName).toList());
        ModuleHealth workflow = health.modules().get(1);
        assertEquals(HealthStatus.UNKNOWN, workflow.status());
        assertEquals("java.lang.IllegalStateException", workflow.details().get("errorType"));
    }

    @Test
    void rejectsDuplicateContributorIdentity() {
        ModuleHealthContributor first = contributor("catalog", () -> health("catalog", HealthStatus.UP));
        ModuleHealthContributor second = contributor(" CATALOG ", () -> health("catalog", HealthStatus.UP));

        assertThrows(IllegalArgumentException.class, () ->
                new CompositeHealthQueryService(CLOCK, List.of(first, second))
        );
    }

    @Test
    void treatsMismatchedContributorResultAsUnknown() {
        ModuleHealthContributor contributor = contributor("catalog", () ->
                health("workflow", HealthStatus.UP)
        );
        CompositeHealthQueryService service = new CompositeHealthQueryService(CLOCK, List.of(contributor));

        ModuleHealth result = service.moduleHealth().getFirst();

        assertEquals("catalog", result.moduleName());
        assertEquals(HealthStatus.UNKNOWN, result.status());
    }

    @Test
    void downStatusTakesPriorityOverUnknownStatus() {
        CompositeHealthQueryService service = new CompositeHealthQueryService(CLOCK, List.of(
                contributor("catalog", () -> health("catalog", HealthStatus.DOWN)),
                contributor("workflow", () -> health("workflow", HealthStatus.UNKNOWN))
        ));

        assertEquals(HealthStatus.DOWN, service.platformHealth().status());
    }

    private static ModuleHealth health(String moduleName, HealthStatus status) {
        return new ModuleHealth(moduleName, status, "", NOW, Map.of());
    }

    private static ModuleHealthContributor contributor(String moduleName, Supplier<ModuleHealth> health) {
        return new ModuleHealthContributor() {
            @Override
            public String moduleName() {
                return moduleName;
            }

            @Override
            public ModuleHealth health() {
                return health.get();
            }
        };
    }

    private record FixedClock(Instant now) implements Clock {
        @Override
        public ZoneId zone() {
            return ZoneOffset.UTC;
        }
    }
}
