package com.datausher.platform.module.api;

import java.util.List;
import java.util.Optional;

public interface ModuleRegistry {
    void register(ModuleDescriptor descriptor);

    void unregister(String moduleName);

    Optional<ModuleDescriptor> findByName(String moduleName);

    List<ModuleDescriptor> listModules();
}
