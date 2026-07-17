package com.datausher.platform.module.api;

import java.util.Map;

public record ModuleCapability(String name, Map<String, String> attributes) {
    public ModuleCapability {
        name = ModuleIdentifiers.normalizeCapabilityName(name);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
