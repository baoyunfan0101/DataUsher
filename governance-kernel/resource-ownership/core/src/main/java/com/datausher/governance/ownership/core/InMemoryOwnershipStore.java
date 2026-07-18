package com.datausher.governance.ownership.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.ownership.api.OwnershipQuery;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.governance.ownership.api.ResourceOwner;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryOwnershipStore implements OwnershipStore {
    private final ConcurrentMap<String, ResourceOwner> owners = new ConcurrentHashMap<>();

    @Override
    public Optional<ResourceOwner> find(ResourceRef resourceRef, SubjectRef subjectRef, OwnershipRole role) {
        return Optional.ofNullable(owners.get(key(resourceRef, subjectRef, role)));
    }

    @Override
    public synchronized void replace(Optional<ResourceOwner> expected, Optional<ResourceOwner> replacement) {
        Objects.requireNonNull(expected, "expected must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");
        if (expected.isEmpty() && replacement.isEmpty()) {
            throw new IllegalArgumentException("expected and replacement must not both be empty");
        }
        String key = key(expected.orElseGet(replacement::orElseThrow));
        replacement.ifPresent(next -> {
            if (!key(next).equals(key)) {
                throw new IllegalArgumentException("ownership keys must match");
            }
        });
        ResourceOwner current = owners.get(key);
        if (!Objects.equals(current, expected.orElse(null))) {
            throw new IllegalStateException("ownership changed concurrently: " + key);
        }
        if (replacement.isPresent()) {
            owners.put(key, replacement.orElseThrow());
        } else {
            owners.remove(key);
        }
    }

    @Override
    public PageResult<ResourceOwner> search(OwnershipQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<ResourceOwner> matches = new ArrayList<>();
        for (ResourceOwner owner : owners.values()) {
            if ((query.resourceRef() == null || query.resourceRef().equals(owner.resourceRef()))
                    && (query.subjectRef() == null || query.subjectRef().equals(owner.subjectRef()))
                    && (query.role() == null || query.role().equals(owner.role()))) {
                matches.add(owner);
            }
        }
        matches.sort(Comparator.comparing(InMemoryOwnershipStore::key));
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = (int) Math.min((long) fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    private static String key(ResourceOwner owner) {
        return key(owner.resourceRef(), owner.subjectRef(), owner.role());
    }

    private static String key(ResourceRef resourceRef, SubjectRef subjectRef, OwnershipRole role) {
        return resourceRef.canonicalValue() + "|" + subjectRef.canonicalValue() + "|" + role.value();
    }
}
