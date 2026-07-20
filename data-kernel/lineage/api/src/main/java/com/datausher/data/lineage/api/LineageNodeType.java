package com.datausher.data.lineage.api;

public record LineageNodeType(String value) {
    public static final LineageNodeType TABLE = new LineageNodeType("table");
    public static final LineageNodeType COLUMN = new LineageNodeType("column");
    public static final LineageNodeType WORKFLOW = new LineageNodeType("workflow");
    public static final LineageNodeType TASK = new LineageNodeType("task");
    public static final LineageNodeType DATASET = new LineageNodeType("dataset");
    public static final LineageNodeType DASHBOARD = new LineageNodeType("dashboard");

    public LineageNodeType {
        value = LineageValues.identifier(value, "value");
    }
}
