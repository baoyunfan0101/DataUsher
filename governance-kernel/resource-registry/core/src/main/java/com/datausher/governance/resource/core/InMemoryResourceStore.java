package com.datausher.governance.resource.core;

import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceTypeDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryResourceStore implements ResourceStore {
    private final ConcurrentMap<String, ResourceTypeDefinition> types = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisteredResource> resources = new ConcurrentHashMap<>();

    @Override
    public ResourceTypeRegistration registerType(ResourceTypeDefinition definition) {
        ResourceTypeDefinition existing = types.putIfAbsent(definition.resourceType(), definition);
        if (existing != null && !existing.equals(definition)) {
            throw new IllegalStateException("resource type already registered differently: "
                    + definition.resourceType());
        }
        return new ResourceTypeRegistration(existing == null ? definition : existing, existing == null);
    }

    @Override
    public void unregisterType(ResourceTypeDefinition definition) {
        if (!types.remove(definition.resourceType(), definition)) {
            throw new IllegalStateException(
                    "resource type changed before rollback: " + definition.resourceType());
        }
    }

    @Override
    public Optional<ResourceTypeDefinition> findType(String resourceType) {
        return Optional.ofNullable(types.get(resourceType));
    }

    @Override
    public List<ResourceTypeDefinition> listTypes() {
        return types.values().stream()
                .sorted(Comparator.comparing(ResourceTypeDefinition::resourceType))
                .toList();
    }

    @Override
    public void create(RegisteredResource resource) {
        RegisteredResource existing = resources.putIfAbsent(resource.ref().canonicalValue(), resource);
        if (existing != null) {
            throw new IllegalStateException("resource already exists: " + resource.ref().canonicalValue());
        }
    }

    @Override
    public void delete(RegisteredResource resource) {
        if (!resources.remove(resource.ref().canonicalValue(), resource)) {
            throw new IllegalStateException(
                    "resource changed before rollback: " + resource.ref().canonicalValue());
        }
    }

    @Override
    public void update(RegisteredResource expected, RegisteredResource replacement) {
        if (!expected.ref().equals(replacement.ref())) {
            throw new IllegalArgumentException("resource references must match");
        }
        if (!resources.replace(expected.ref().canonicalValue(), expected, replacement)) {
            throw new IllegalStateException(
                    "resource changed concurrently: " + expected.ref().canonicalValue());
        }
    }

    @Override
    public Optional<RegisteredResource> find(ResourceRef ref) {
        return Optional.ofNullable(resources.get(ref.canonicalValue()));
    }

    @Override
    public List<RegisteredResource> search(ResourceQuery query) {
        List<RegisteredResource> matches = new ArrayList<>();
        for (RegisteredResource resource : resources.values()) {
            if ((query.resourceType() == null || query.resourceType().equals(resource.ref().resourceType()))
                    && (query.scope() == null || query.scope().equals(resource.ref().scope()))
                    && (query.lifecycle() == null || query.lifecycle() == resource.lifecycle())) {
                matches.add(resource);
            }
        }
        matches.sort(Comparator.comparing(resource -> resource.ref().canonicalValue()));
        return List.copyOf(matches);
    }
}
