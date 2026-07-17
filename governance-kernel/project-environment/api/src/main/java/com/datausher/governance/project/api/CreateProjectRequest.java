package com.datausher.governance.project.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record CreateProjectRequest(
        String key,
        String displayName,
        List<EnvironmentSpec> environments,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public CreateProjectRequest {
        key = new EnvironmentSpec(key, displayName).key();
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        environments = List.copyOf(Objects.requireNonNull(environments, "environments must not be null"));
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (environments.isEmpty()) {
            throw new IllegalArgumentException("environments must not be empty");
        }
        Set<String> keys = new HashSet<>();
        for (EnvironmentSpec environment : environments) {
            if (!keys.add(environment.key())) {
                throw new IllegalArgumentException("duplicate environment key: " + environment.key());
            }
        }
    }
}
