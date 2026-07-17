package com.datausher.governance.access.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.List;
import java.util.Set;

public interface AccessPolicyStore {
    void save(AccessPolicy policy);

    List<AccessPolicy> findMatching(Set<SubjectRef> subjects, String action, ResourceRef resource);
}
