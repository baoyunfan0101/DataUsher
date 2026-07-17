package com.datausher.platform.config.api;

import java.util.Objects;

public record ConfigResolutionContext(
        ConfigNamespace namespace,
        ConfigProfile profile
) {
    public static final ConfigResolutionContext GLOBAL =
            new ConfigResolutionContext(ConfigNamespace.GLOBAL, ConfigProfile.DEFAULT);

    public ConfigResolutionContext {
        namespace = Objects.requireNonNull(namespace, "namespace must not be null");
        profile = Objects.requireNonNull(profile, "profile must not be null");
    }

    public static ConfigResolutionContext of(ConfigNamespace namespace, ConfigProfile profile) {
        return new ConfigResolutionContext(namespace, profile);
    }
}
