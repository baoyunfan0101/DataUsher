package com.datausher.governance.resource.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface ResourceQueryService {
    Optional<RegisteredResource> find(ResourceRef ref);

    PageResult<RegisteredResource> search(ResourceQuery query, PageRequest pageRequest);
}
