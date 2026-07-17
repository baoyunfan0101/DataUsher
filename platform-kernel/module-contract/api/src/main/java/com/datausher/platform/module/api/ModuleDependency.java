package com.datausher.platform.module.api;

public record ModuleDependency(String moduleName, String minimumVersion, boolean required) {
    public ModuleDependency {
        moduleName = ModuleIdentifiers.normalizeModuleName(moduleName);
        minimumVersion = minimumVersion == null ? "" : minimumVersion.trim();
    }
}
