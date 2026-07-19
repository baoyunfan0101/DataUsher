package com.datausher.data.lineage.api;

public record LineageEdgeType(String value) {
    public static final LineageEdgeType DATA_FLOW = new LineageEdgeType("data-flow");
    public static final LineageEdgeType READS = new LineageEdgeType("reads");
    public static final LineageEdgeType WRITES = new LineageEdgeType("writes");
    public static final LineageEdgeType DERIVES_FROM = new LineageEdgeType("derives-from");
    public static final LineageEdgeType CONTAINS = new LineageEdgeType("contains");

    public LineageEdgeType {
        value = LineageValues.identifier(value, "value");
    }
}
