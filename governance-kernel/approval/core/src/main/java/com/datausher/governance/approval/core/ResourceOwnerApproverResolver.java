package com.datausher.governance.approval.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.approval.api.ApproverSelector;
import com.datausher.governance.approval.api.ApproverSelectorResolver;
import com.datausher.governance.approval.api.ApproverSelectorType;
import com.datausher.governance.ownership.api.OwnershipQuery;
import com.datausher.governance.ownership.api.OwnershipQueryService;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.governance.ownership.api.ResourceOwner;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.page.PageRequest;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResourceOwnerApproverResolver implements ApproverSelectorResolver {
    private final OwnershipQueryService ownership;

    public ResourceOwnerApproverResolver(OwnershipQueryService ownership) {
        this.ownership = Objects.requireNonNull(ownership, "ownership must not be null");
    }

    @Override
    public ApproverSelectorType selectorType() {
        return ApproverSelectorType.RESOURCE_OWNER;
    }

    @Override
    public Set<SubjectRef> resolve(ApproverSelector selector, ResourceRef targetResource) {
        if (!ApproverSelectorType.RESOURCE_OWNER.equals(selector.type())) {
            throw new IllegalArgumentException("unsupported selector type: " + selector.type());
        }
        String role = selector.criteria().get("role");
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("selector criterion is required: role");
        }
        return ownership.search(
                        new OwnershipQuery(targetResource, null, new OwnershipRole(role)),
                        new PageRequest(1, 1000, List.of())
                ).items().stream()
                .map(ResourceOwner::subjectRef)
                .collect(Collectors.toUnmodifiableSet());
    }
}
