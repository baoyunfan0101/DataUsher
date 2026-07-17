package com.datausher.governance.access.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAccessPolicyStore implements AccessPolicyStore {
    private final ConcurrentMap<String, AccessPolicy> policies = new ConcurrentHashMap<>();

    @Override
    public void save(AccessPolicy policy) {
        policies.put(policy.policyId(), policy);
    }

    @Override
    public List<AccessPolicy> findMatching(Set<SubjectRef> subjects, String action, ResourceRef resource) {
        return policies.values().stream()
                .filter(policy -> subjects.stream().anyMatch(subject -> policy.matches(subject, action, resource)))
                .sorted(Comparator.comparingInt(AccessPolicy::priority).reversed()
                        .thenComparing(policy -> policy.effect() == PolicyEffect.DENY ? 0 : 1)
                        .thenComparing(AccessPolicy::policyId))
                .toList();
    }
}
