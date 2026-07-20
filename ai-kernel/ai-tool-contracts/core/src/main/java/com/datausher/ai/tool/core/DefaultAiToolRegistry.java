package com.datausher.ai.tool.core;

import com.datausher.ai.tool.api.AiToolCatalogQuery;
import com.datausher.ai.tool.api.AiToolCatalogService;
import com.datausher.ai.tool.api.AiToolDefinition;
import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.ai.tool.api.AiToolRegistry;
import com.datausher.ai.tool.api.AiToolSchema;
import com.datausher.ai.tool.api.ChangeAiToolStatusRequest;
import com.datausher.ai.tool.api.RegisterAiToolRequest;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public final class DefaultAiToolRegistry implements AiToolRegistry, AiToolCatalogService {
    private final AiToolStore store;

    public DefaultAiToolRegistry(AiToolStore store) {
        this.store = java.util.Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public AiToolDefinition register(RegisterAiToolRequest request) {
        AiToolDefinition definition = new AiToolDefinition(
                request.schema(), request.permissionPolicy());
        return store.create(definition);
    }

    @Override
    public AiToolDefinition changeStatus(ChangeAiToolStatusRequest request) {
        AiToolDefinition current = store.find(request.ref())
                .orElseThrow(() -> new IllegalArgumentException("AI tool does not exist"));
        if (current.schema().revision() != request.expectedRevision()) {
            throw new com.datausher.platform.shared.concurrent.RevisionConflictException(
                    "ai-tool", request.ref().toString(),
                    request.expectedRevision(), current.schema().revision());
        }
        AiToolSchema currentSchema = current.schema();
        AiToolSchema replacementSchema = new AiToolSchema(
                currentSchema.ref(), currentSchema.displayName(), currentSchema.description(),
                currentSchema.parameters(), request.status(), currentSchema.attributes(),
                currentSchema.revision() + 1);
        return store.update(current, new AiToolDefinition(
                replacementSchema, current.permissionPolicy()));
    }

    @Override
    public Optional<AiToolDefinition> find(AiToolRef ref) {
        return store.find(ref);
    }

    @Override
    public Optional<AiToolDefinition> findLatest(AiToolId toolId) {
        return store.findLatest(toolId);
    }

    @Override
    public PageResult<AiToolDefinition> search(
            AiToolCatalogQuery query,
            PageRequest pageRequest
    ) {
        return store.search(query, pageRequest);
    }
}
