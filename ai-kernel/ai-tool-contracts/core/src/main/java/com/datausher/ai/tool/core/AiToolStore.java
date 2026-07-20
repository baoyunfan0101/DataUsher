package com.datausher.ai.tool.core;

import com.datausher.ai.tool.api.AiToolCatalogQuery;
import com.datausher.ai.tool.api.AiToolDefinition;
import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface AiToolStore {
    AiToolDefinition create(AiToolDefinition definition);

    AiToolDefinition update(AiToolDefinition expected, AiToolDefinition replacement);

    Optional<AiToolDefinition> find(AiToolRef ref);

    Optional<AiToolDefinition> findLatest(AiToolId toolId);

    PageResult<AiToolDefinition> search(AiToolCatalogQuery query, PageRequest pageRequest);
}
