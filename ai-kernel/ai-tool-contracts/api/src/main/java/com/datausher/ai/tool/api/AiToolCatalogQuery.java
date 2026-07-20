package com.datausher.ai.tool.api;

import java.util.Set;

public record AiToolCatalogQuery(Set<AiToolStatus> statuses) {
    public AiToolCatalogQuery {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    }

    public static AiToolCatalogQuery all() {
        return new AiToolCatalogQuery(Set.of());
    }
}
