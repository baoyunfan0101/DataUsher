package com.datausher.ai.context.core;

import com.datausher.ai.context.api.AiAssembledContext;
import com.datausher.ai.context.api.AiContextAssembler;
import com.datausher.ai.context.api.AiContextAssemblyRequest;
import com.datausher.ai.context.api.AiContextItem;
import com.datausher.ai.context.api.AiContextSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultAiContextAssembler implements AiContextAssembler {
    @Override
    public AiAssembledContext assemble(AiContextAssemblyRequest request) {
        Map<String, List<AiContextItem>> grouped = new LinkedHashMap<>();
        int estimatedTokens = 0;
        boolean truncated = false;
        List<AiContextItem> ordered = request.items().stream()
                .sorted(Comparator.comparingInt(AiContextItem::priority).reversed()
                        .thenComparing(AiContextItem::title))
                .toList();
        for (AiContextItem item : ordered) {
            int itemTokens = estimateTokens(item);
            if (estimatedTokens + itemTokens > request.tokenBudget()) {
                truncated = true;
                continue;
            }
            estimatedTokens += itemTokens;
            grouped.computeIfAbsent(item.source().type().value(), ignored -> new ArrayList<>())
                    .add(item);
        }
        List<AiContextSection> sections = grouped.entrySet().stream()
                .map(entry -> new AiContextSection(entry.getKey(), entry.getValue(), Map.of()))
                .toList();
        return new AiAssembledContext(sections, estimatedTokens, truncated, Map.of());
    }

    private static int estimateTokens(AiContextItem item) {
        return Math.max(1, (item.title().length() + item.content().length() + 3) / 4);
    }
}
