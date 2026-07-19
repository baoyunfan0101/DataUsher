package com.datausher.data.lineage.api;

import java.util.Set;

public record LineageNodeQuery(Set<LineageNodeType> types, String text) {
    public LineageNodeQuery {
        types = types == null ? Set.of() : Set.copyOf(types);
        text = text == null ? "" : text.trim();
    }

    public static LineageNodeQuery all() {
        return new LineageNodeQuery(Set.of(), "");
    }
}
