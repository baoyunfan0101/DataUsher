package com.datausher.governance.access.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.Optional;
import java.util.Set;

public interface AccessPolicyStore {
    void save(AccessPolicy policy);

    Optional<AccessPolicy> findEffective(
            Set<SubjectRef> subjects,
            String action,
            ResourceRef resource
    );
}
