package com.datausher.platform.module.core;

import com.datausher.platform.module.api.ModuleDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryModuleRegistryTest {
    @Test
    void acceptsIdenticalRegistrationIdempotently() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleDescriptor descriptor = descriptor("catalog", "1.0.0");

        registry.register(descriptor);
        registry.register(descriptor);

        assertEquals(1, registry.listModules().size());
        assertSame(descriptor, registry.findByName("catalog").orElseThrow());
    }

    @Test
    void rejectsConflictingRegistrationWithoutReplacingExistingDescriptor() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleDescriptor existing = descriptor("catalog", "1.0.0");
        ModuleDescriptor conflicting = descriptor("catalog", "2.0.0");
        registry.register(existing);

        assertThrows(IllegalStateException.class, () -> registry.register(conflicting));
        assertSame(existing, registry.findByName("catalog").orElseThrow());
    }

    @Test
    void returnsModulesInStableNameOrder() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        registry.register(descriptor("workflow", "1.0.0"));
        registry.register(descriptor("catalog", "1.0.0"));

        assertEquals(
                List.of("catalog", "workflow"),
                registry.listModules().stream().map(ModuleDescriptor::name).toList()
        );
    }

    @Test
    void resolvesCanonicalModuleIdentity() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleDescriptor descriptor = descriptor("Catalog.Core", "1.0.0");
        registry.register(descriptor);

        assertSame(descriptor, registry.findByName(" CATALOG.CORE ").orElseThrow());
        registry.unregister("catalog.core");
        assertEquals(List.of(), registry.listModules());
    }

    private static ModuleDescriptor descriptor(String name, String version) {
        return new ModuleDescriptor(name, version, "", List.of(), List.of(), Map.of());
    }
}
