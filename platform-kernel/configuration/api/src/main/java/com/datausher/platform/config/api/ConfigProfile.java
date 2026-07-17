package com.datausher.platform.config.api;

public record ConfigProfile(String name) {
    public static final ConfigProfile DEFAULT = new ConfigProfile("default");

    public ConfigProfile {
        name = ConfigNames.normalizeSegment(name);
    }

    public static ConfigProfile of(String name) {
        return new ConfigProfile(name);
    }
}
