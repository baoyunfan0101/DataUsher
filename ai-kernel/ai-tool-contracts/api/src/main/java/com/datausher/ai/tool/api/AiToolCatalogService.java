package com.datausher.ai.tool.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface AiToolCatalogService {
    Optional<AiToolDefinition> find(AiToolRef ref);

    Optional<AiToolDefinition> findLatest(AiToolId toolId);

    PageResult<AiToolDefinition> search(AiToolCatalogQuery query, PageRequest pageRequest);
}
