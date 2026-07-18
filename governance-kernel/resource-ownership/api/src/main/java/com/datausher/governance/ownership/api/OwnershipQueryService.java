package com.datausher.governance.ownership.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;

public interface OwnershipQueryService {
    List<ResourceOwner> listOwners(ResourceRef resourceRef);

    PageResult<ResourceOwner> search(OwnershipQuery query, PageRequest pageRequest);
}
