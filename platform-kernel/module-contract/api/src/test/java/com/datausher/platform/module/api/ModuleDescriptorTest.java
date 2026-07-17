package com.datausher.platform.module.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleDescriptorTest {
    @Test
    void normalizesStableIdentifiers() {
        ModuleDescriptor descriptor = descriptor(
                " Catalog.Core ",
                List.of(new ModuleDependency(" Identity.Core ", "1.0.0", true)),
                List.of(new ModuleCapability(" Full.Text_Search ", Map.of()))
        );

        assertEquals("catalog.core", descriptor.name());
        assertEquals("identity.core", descriptor.dependencies().getFirst().moduleName());
        assertEquals("full.text_search", descriptor.capabilities().getFirst().name());
    }

    @Test
    void rejectsMalformedIdentifiers() {
        assertThrows(IllegalArgumentException.class, () ->
                descriptor("catalog..core", List.of(), List.of())
        );
        assertThrows(IllegalArgumentException.class, () ->
                new ModuleCapability("catalog/search", Map.of())
        );
    }

    @Test
    void rejectsSelfDependency() {
        ModuleDependency dependency = new ModuleDependency("catalog", "1.0.0", true);

        assertThrows(IllegalArgumentException.class, () ->
                descriptor("catalog", List.of(dependency), List.of())
        );
    }

    @Test
    void rejectsDuplicateDependencies() {
        ModuleDependency required = new ModuleDependency("identity", "1.0.0", true);
        ModuleDependency optional = new ModuleDependency("identity", "2.0.0", false);

        assertThrows(IllegalArgumentException.class, () ->
                descriptor("catalog", List.of(required, optional), List.of())
        );
    }

    @Test
    void rejectsDuplicateCapabilities() {
        ModuleCapability first = new ModuleCapability("search", Map.of("mode", "exact"));
        ModuleCapability second = new ModuleCapability("search", Map.of("mode", "fuzzy"));

        assertThrows(IllegalArgumentException.class, () ->
                descriptor("catalog", List.of(), List.of(first, second))
        );
    }

    private static ModuleDescriptor descriptor(
            String name,
            List<ModuleDependency> dependencies,
            List<ModuleCapability> capabilities
    ) {
        return new ModuleDescriptor(name, "1.0.0", "", dependencies, capabilities, Map.of());
    }
}
