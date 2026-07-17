package com.datausher.platform.config.api;

public record ConfigNamespace(String name) {
    public static final ConfigNamespace GLOBAL = new ConfigNamespace("global");

    public ConfigNamespace {
        name = ConfigNames.normalizeSegment(name);
    }

    public static ConfigNamespace of(String name) {
        return new ConfigNamespace(name);
    }
}
