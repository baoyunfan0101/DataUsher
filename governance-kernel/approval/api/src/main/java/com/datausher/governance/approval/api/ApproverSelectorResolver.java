package com.datausher.governance.approval.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.Set;

public interface ApproverSelectorResolver {
    ApproverSelectorType selectorType();

    Set<SubjectRef> resolve(ApproverSelector selector, ResourceRef targetResource);
}
