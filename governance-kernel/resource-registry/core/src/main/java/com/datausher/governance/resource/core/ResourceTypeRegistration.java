package com.datausher.governance.resource.core;

import com.datausher.governance.resource.api.ResourceTypeDefinition;

import java.util.Objects;

public record ResourceTypeRegistration(ResourceTypeDefinition definition, boolean created) {
    public ResourceTypeRegistration {
        definition = Objects.requireNonNull(definition, "definition must not be null");
    }
}
