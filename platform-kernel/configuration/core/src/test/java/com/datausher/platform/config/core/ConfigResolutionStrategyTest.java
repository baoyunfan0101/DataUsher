package com.datausher.platform.config.core;

import com.datausher.platform.config.api.ConfigKey;
import com.datausher.platform.config.api.ConfigNamespace;
import com.datausher.platform.config.api.ConfigProfile;
import com.datausher.platform.config.api.ConfigResolutionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigResolutionStrategyTest {
    @Test
    void producesStableSpecificToGlobalCandidateOrder() {
        ConfigResolutionStrategy strategy = ConfigResolutionStrategy.defaultStrategy();
        ConfigResolutionContext context = ConfigResolutionContext.of(
                ConfigNamespace.of("catalog"),
                ConfigProfile.of("production")
        );

        assertEquals(List.of(
                "catalog.production.feature.enabled",
                "catalog.default.feature.enabled",
                "catalog.feature.enabled",
                "global.production.feature.enabled",
                "global.default.feature.enabled",
                "global.feature.enabled",
                "feature.enabled"
        ), strategy.candidates(ConfigKey.of("feature.enabled"), context));
    }

    @Test
    void requiresExplicitResolutionContext() {
        assertThrows(NullPointerException.class, () ->
                ConfigResolutionStrategy.defaultStrategy().candidates(ConfigKey.of("feature.enabled"), null)
        );
    }

    @Test
    void removesDuplicateFallbackCandidates() {
        assertEquals(List.of(
                "global.default.feature.enabled",
                "global.feature.enabled",
                "feature.enabled"
        ), ConfigResolutionStrategy.defaultStrategy().candidates(
                ConfigKey.of("feature.enabled"),
                ConfigResolutionContext.GLOBAL
        ));
    }
}
