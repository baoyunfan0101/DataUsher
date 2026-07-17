package com.datausher.platform.config.api;

public record ConfigKey(String name) {
    public ConfigKey {
        name = ConfigNames.normalizeKey(name);
    }

    public static ConfigKey of(String name) {
        return new ConfigKey(name);
    }
}
