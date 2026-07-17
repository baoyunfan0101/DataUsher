package com.datausher.platform.module.core;

import com.datausher.platform.module.api.HealthQueryService;
import com.datausher.platform.module.api.HealthStatus;
import com.datausher.platform.module.api.ModuleHealth;
import com.datausher.platform.module.api.ModuleHealthContributor;
import com.datausher.platform.module.api.ModuleIdentifiers;
import com.datausher.platform.module.api.PlatformHealth;
import com.datausher.platform.shared.time.Clock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CompositeHealthQueryService implements HealthQueryService {
    private final Clock clock;
    private final List<NamedContributor> contributors;

    public CompositeHealthQueryService(Clock clock, List<ModuleHealthContributor> contributors) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.contributors = validateContributors(contributors);
    }

    @Override
    public PlatformHealth platformHealth() {
        List<ModuleHealth> health = moduleHealth();
        HealthStatus status = summarize(health);
        return new PlatformHealth(status, clock.now(), health);
    }

    @Override
    public List<ModuleHealth> moduleHealth() {
        return contributors.stream()
                .map(this::checkHealth)
                .sorted(Comparator.comparing(ModuleHealth::moduleName))
                .toList();
    }

    private ModuleHealth checkHealth(NamedContributor namedContributor) {
        try {
            ModuleHealth health = Objects.requireNonNull(
                    namedContributor.contributor().health(),
                    "health contributor must not return null"
            );
            if (!namedContributor.moduleName().equals(health.moduleName())) {
                throw new IllegalStateException("health contributor returned a different module name");
            }
            return health;
        } catch (RuntimeException failure) {
            return new ModuleHealth(
                    namedContributor.moduleName(),
                    HealthStatus.UNKNOWN,
                    "health check failed",
                    clock.now(),
                    Map.of("errorType", failure.getClass().getName())
            );
        }
    }

    private static HealthStatus summarize(List<ModuleHealth> health) {
        if (health.isEmpty()) {
            return HealthStatus.UNKNOWN;
        }
        if (health.stream().anyMatch(item -> item.status() == HealthStatus.DOWN)) {
            return HealthStatus.DOWN;
        }
        if (health.stream().anyMatch(item -> item.status() == HealthStatus.UNKNOWN)) {
            return HealthStatus.UNKNOWN;
        }
        if (health.stream().anyMatch(item -> item.status() == HealthStatus.DEGRADED)) {
            return HealthStatus.DEGRADED;
        }
        return HealthStatus.UP;
    }

    private static List<NamedContributor> validateContributors(List<ModuleHealthContributor> contributors) {
        Objects.requireNonNull(contributors, "contributors must not be null");
        Map<String, ModuleHealthContributor> contributorsByName = new LinkedHashMap<>();
        for (ModuleHealthContributor contributor : contributors) {
            Objects.requireNonNull(contributor, "contributor must not be null");
            String moduleName = normalizeName(contributor.moduleName());
            if (contributorsByName.putIfAbsent(moduleName, contributor) != null) {
                throw new IllegalArgumentException("duplicate health contributor: " + moduleName);
            }
        }

        List<NamedContributor> validated = new ArrayList<>(contributorsByName.size());
        contributorsByName.forEach((moduleName, contributor) ->
                validated.add(new NamedContributor(moduleName, contributor))
        );
        return List.copyOf(validated);
    }

    private static String normalizeName(String moduleName) {
        return ModuleIdentifiers.normalizeModuleName(moduleName);
    }

    private record NamedContributor(String moduleName, ModuleHealthContributor contributor) {
    }
}
