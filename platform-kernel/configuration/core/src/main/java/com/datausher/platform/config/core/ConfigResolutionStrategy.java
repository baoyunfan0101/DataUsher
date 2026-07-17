package com.datausher.platform.config.core;

import com.datausher.platform.config.api.ConfigKey;
import com.datausher.platform.config.api.ConfigNamespace;
import com.datausher.platform.config.api.ConfigProfile;
import com.datausher.platform.config.api.ConfigResolutionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ConfigResolutionStrategy(
        ConfigNamespace fallbackNamespace,
        ConfigProfile fallbackProfile
) {
    public ConfigResolutionStrategy {
        fallbackNamespace = Objects.requireNonNull(fallbackNamespace, "fallbackNamespace must not be null");
        fallbackProfile = Objects.requireNonNull(fallbackProfile, "fallbackProfile must not be null");
    }

    public static ConfigResolutionStrategy defaultStrategy() {
        return new ConfigResolutionStrategy(ConfigNamespace.GLOBAL, ConfigProfile.DEFAULT);
    }

    public List<String> candidates(ConfigKey key, ConfigResolutionContext context) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Set<String> candidates = new LinkedHashSet<>();

        addCandidate(candidates, context.namespace(), context.profile(), key);
        addCandidate(candidates, context.namespace(), fallbackProfile, key);
        addCandidate(candidates, context.namespace(), null, key);
        addCandidate(candidates, fallbackNamespace, context.profile(), key);
        addCandidate(candidates, fallbackNamespace, fallbackProfile, key);
        addCandidate(candidates, fallbackNamespace, null, key);
        candidates.add(key.name());
        return List.copyOf(candidates);
    }

    private static void addCandidate(
            Set<String> candidates,
            ConfigNamespace namespace,
            ConfigProfile profile,
            ConfigKey key
    ) {
        String profileSegment = profile == null ? "" : "." + profile.name();
        candidates.add(namespace.name() + profileSegment + "." + key.name());
    }
}
