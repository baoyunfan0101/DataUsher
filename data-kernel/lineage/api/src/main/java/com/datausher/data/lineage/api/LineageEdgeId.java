package com.datausher.data.lineage.api;

public record LineageEdgeId(String value) implements Comparable<LineageEdgeId> {
    public LineageEdgeId {
        value = LineageValues.identifier(value, "value");
    }

    @Override
    public int compareTo(LineageEdgeId other) {
        return value.compareTo(other.value);
    }
}
