package com.datausher.governance.ownership.core;

import com.datausher.governance.ownership.api.OwnershipQuery;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.governance.ownership.api.ResourceOwner;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface OwnershipStore {
    Optional<ResourceOwner> find(ResourceRef resourceRef, SubjectRef subjectRef, OwnershipRole role);

    void replace(Optional<ResourceOwner> expected, Optional<ResourceOwner> replacement);

    PageResult<ResourceOwner> search(OwnershipQuery query, PageRequest pageRequest);
}
