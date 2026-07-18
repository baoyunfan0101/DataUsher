package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.approval.api.ApproverSelector;
import com.datausher.governance.approval.api.ApproverSelectorResolver;
import com.datausher.governance.approval.api.ApproverSelectorType;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.Set;

public final class DirectSubjectApproverResolver implements ApproverSelectorResolver {
    @Override
    public ApproverSelectorType selectorType() {
        return ApproverSelectorType.SUBJECT;
    }

    @Override
    public Set<SubjectRef> resolve(ApproverSelector selector, ResourceRef targetResource) {
        requireType(selector);
        String subjectType = requiredCriterion(selector, "subjectType");
        String subjectId = requiredCriterion(selector, "subjectId");
        return Set.of(new SubjectRef(new SubjectType(subjectType), subjectId));
    }

    private static void requireType(ApproverSelector selector) {
        if (!ApproverSelectorType.SUBJECT.equals(selector.type())) {
            throw new IllegalArgumentException("unsupported selector type: " + selector.type());
        }
    }

    private static String requiredCriterion(ApproverSelector selector, String name) {
        String value = selector.criteria().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("selector criterion is required: " + name);
        }
        return value;
    }
}
