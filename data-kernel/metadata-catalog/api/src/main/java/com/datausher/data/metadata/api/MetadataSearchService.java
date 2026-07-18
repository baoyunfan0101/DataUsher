package com.datausher.data.metadata.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

public interface MetadataSearchService {
    PageResult<MetadataSearchHit> search(MetadataSearchQuery query, PageRequest pageRequest);
}
