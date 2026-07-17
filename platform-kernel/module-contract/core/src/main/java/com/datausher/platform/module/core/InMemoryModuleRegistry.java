package com.datausher.platform.module.core;

import com.datausher.platform.module.api.ModuleDescriptor;
import com.datausher.platform.module.api.ModuleIdentifiers;
import com.datausher.platform.module.api.ModuleRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryModuleRegistry implements ModuleRegistry {
    private final ConcurrentMap<String, ModuleDescriptor> modules = new ConcurrentHashMap<>();

    @Override
    public void register(ModuleDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        modules.compute(descriptor.name(), (moduleName, existing) -> {
            if (existing == null || existing.equals(descriptor)) {
                return descriptor;
            }
            throw new IllegalStateException("module is already registered with a different descriptor: " + moduleName);
        });
    }

    @Override
    public void unregister(String moduleName) {
        modules.remove(normalizeName(moduleName));
    }

    @Override
    public Optional<ModuleDescriptor> findByName(String moduleName) {
        return Optional.ofNullable(modules.get(normalizeName(moduleName)));
    }

    @Override
    public List<ModuleDescriptor> listModules() {
        return modules.values().stream()
                .sorted(Comparator.comparing(ModuleDescriptor::name))
                .toList();
    }

    private static String normalizeName(String moduleName) {
        return ModuleIdentifiers.normalizeModuleName(moduleName);
    }
}
