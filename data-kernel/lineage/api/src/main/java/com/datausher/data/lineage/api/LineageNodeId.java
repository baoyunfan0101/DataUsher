package com.datausher.data.lineage.api;

public record LineageNodeId(String value) implements Comparable<LineageNodeId> {
    public LineageNodeId {
        value = LineageValues.identifier(value, "value");
    }

    @Override
    public int compareTo(LineageNodeId other) {
        return value.compareTo(other.value);
    }
}
