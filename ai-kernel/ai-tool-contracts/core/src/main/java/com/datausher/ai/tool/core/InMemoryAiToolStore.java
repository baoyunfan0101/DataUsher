package com.datausher.ai.tool.core;

import com.datausher.ai.tool.api.AiToolCatalogQuery;
import com.datausher.ai.tool.api.AiToolDefinition;
import com.datausher.ai.tool.api.AiToolId;
import com.datausher.ai.tool.api.AiToolRef;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAiToolStore implements AiToolStore {
    private Map<AiToolRef, AiToolDefinition> definitions = Map.of();

    @Override
    public synchronized AiToolDefinition create(AiToolDefinition definition) {
        if (definitions.containsKey(definition.schema().ref())) {
            throw new RevisionConflictException(
                    "ai-tool", definition.schema().ref().toString(),
                    definition.schema().revision(),
                    definitions.get(definition.schema().ref()).schema().revision());
        }
        Map<AiToolRef, AiToolDefinition> next = new LinkedHashMap<>(definitions);
        next.put(definition.schema().ref(), definition);
        definitions = Map.copyOf(next);
        return definition;
    }

    @Override
    public synchronized AiToolDefinition update(
            AiToolDefinition expected,
            AiToolDefinition replacement
    ) {
        AiToolDefinition current = definitions.get(expected.schema().ref());
        if (!expected.equals(current)) {
            long currentRevision = current == null ? 0 : current.schema().revision();
            throw new RevisionConflictException(
                    "ai-tool", expected.schema().ref().toString(),
                    expected.schema().revision(), currentRevision);
        }
        Map<AiToolRef, AiToolDefinition> next = new LinkedHashMap<>(definitions);
        next.put(replacement.schema().ref(), replacement);
        definitions = Map.copyOf(next);
        return replacement;
    }

    @Override
    public synchronized Optional<AiToolDefinition> find(AiToolRef ref) {
        return Optional.ofNullable(definitions.get(ref));
    }

    @Override
    public synchronized Optional<AiToolDefinition> findLatest(AiToolId toolId) {
        return definitions.values().stream()
                .filter(definition -> definition.schema().ref().toolId().equals(toolId))
                .max(Comparator.comparingLong(definition -> definition.schema().ref().version()));
    }

    @Override
    public synchronized PageResult<AiToolDefinition> search(
            AiToolCatalogQuery query,
            PageRequest pageRequest
    ) {
        List<AiToolDefinition> values = definitions.values().stream()
                .filter(definition -> query.statuses().isEmpty()
                        || query.statuses().contains(definition.schema().status()))
                .sorted(Comparator
                        .comparing((AiToolDefinition definition) -> definition.schema().ref().toolId())
                        .thenComparingLong(definition -> definition.schema().ref().version()))
                .toList();
        int fromIndex = (int) Math.min(pageRequest.offset(), values.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), values.size());
        return new PageResult<>(
                values.subList(fromIndex, toIndex), values.size(),
                pageRequest.page(), pageRequest.size());
    }
}
