package com.datausher.platform.config.core;

import com.datausher.platform.config.api.ConfigKey;
import com.datausher.platform.config.api.ConfigNamespace;
import com.datausher.platform.config.api.ConfigProfile;
import com.datausher.platform.config.api.ConfigResolutionContext;
import com.datausher.platform.config.api.ConfigValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapConfigQueryServiceTest {
    @Test
    void resolvesMostSpecificCandidateAndReportsItsSource() {
        MapConfigQueryService service = new MapConfigQueryService(Map.of(
                "catalog.production.feature.enabled", "true",
                "global.default.feature.enabled", "false",
                "feature.enabled", "false"
        ), "bootstrap");
        ConfigResolutionContext context = ConfigResolutionContext.of(
                ConfigNamespace.of("catalog"),
                ConfigProfile.of("production")
        );

        ConfigValue value = service.find(ConfigKey.of("feature.enabled"), context).orElseThrow();

        assertEquals("true", value.value());
        assertEquals("bootstrap:catalog.production.feature.enabled", value.source());
    }

    @Test
    void rejectsAmbiguousOrImplicitInputs() {
        assertThrows(NullPointerException.class, () -> new MapConfigQueryService(null));
        assertThrows(IllegalArgumentException.class, () ->
                new MapConfigQueryService(Map.of("Catalog.Feature.Enabled", "true"))
        );
        MapConfigQueryService service = new MapConfigQueryService(Map.of());
        assertThrows(NullPointerException.class, () ->
                service.find(ConfigKey.of("feature.enabled"), null)
        );
    }

    @Test
    void normalizesTypedLookupIdentifiers() {
        MapConfigQueryService service = new MapConfigQueryService(Map.of(
                "catalog.production.feature.enabled", "true"
        ));
        ConfigResolutionContext context = ConfigResolutionContext.of(
                ConfigNamespace.of(" CATALOG "),
                ConfigProfile.of(" PRODUCTION ")
        );

        assertEquals(true, service.getBoolean(ConfigKey.of(" FEATURE.ENABLED "), context, false));
    }
}
