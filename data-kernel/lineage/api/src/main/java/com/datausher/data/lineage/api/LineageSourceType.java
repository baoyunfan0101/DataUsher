package com.datausher.data.lineage.api;

public record LineageSourceType(String value) {
    public static final LineageSourceType EXECUTION = new LineageSourceType("execution");
    public static final LineageSourceType WORKFLOW = new LineageSourceType("workflow");
    public static final LineageSourceType PARSER = new LineageSourceType("parser");
    public static final LineageSourceType MANUAL = new LineageSourceType("manual");

    public LineageSourceType {
        value = LineageValues.identifier(value, "value");
    }
}
