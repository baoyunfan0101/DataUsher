package com.datausher.governance.resource.api;

import java.util.List;
import java.util.Optional;

public interface ResourceTypeRegistry {
    ResourceTypeDefinition register(RegisterResourceTypeRequest request);

    Optional<ResourceTypeDefinition> find(String resourceType);

    List<ResourceTypeDefinition> list();
}
