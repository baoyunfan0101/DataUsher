package com.datausher.platform.module.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ModuleDescriptor(
        String name,
        String version,
        String description,
        List<ModuleDependency> dependencies,
        List<ModuleCapability> capabilities,
        Map<String, String> attributes
) {
    public ModuleDescriptor {
        name = ModuleIdentifiers.normalizeModuleName(name);
        version = Objects.requireNonNull(version, "version must not be null").trim();
        description = description == null ? "" : description.trim();
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (version.isEmpty()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        validateDependencies(name, dependencies);
        validateCapabilities(capabilities);
    }

    private static void validateDependencies(String moduleName, List<ModuleDependency> dependencies) {
        Set<String> dependencyNames = new HashSet<>();
        for (ModuleDependency dependency : dependencies) {
            if (moduleName.equals(dependency.moduleName())) {
                throw new IllegalArgumentException("module must not depend on itself: " + moduleName);
            }
            if (!dependencyNames.add(dependency.moduleName())) {
                throw new IllegalArgumentException("duplicate module dependency: " + dependency.moduleName());
            }
        }
    }

    private static void validateCapabilities(List<ModuleCapability> capabilities) {
        Set<String> capabilityNames = new HashSet<>();
        for (ModuleCapability capability : capabilities) {
            if (!capabilityNames.add(capability.name())) {
                throw new IllegalArgumentException("duplicate module capability: " + capability.name());
            }
        }
    }
}
