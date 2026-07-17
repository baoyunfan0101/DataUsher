package com.datausher.governance.resource.core;

import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceTypeDefinition;

import java.util.List;
import java.util.Optional;

public interface ResourceStore {
    ResourceTypeRegistration registerType(ResourceTypeDefinition definition);

    void unregisterType(ResourceTypeDefinition definition);

    Optional<ResourceTypeDefinition> findType(String resourceType);

    List<ResourceTypeDefinition> listTypes();

    void create(RegisteredResource resource);

    void delete(RegisteredResource resource);

    void update(RegisteredResource expected, RegisteredResource replacement);

    Optional<RegisteredResource> find(ResourceRef ref);

    List<RegisteredResource> search(ResourceQuery query);
}
