package com.datausher.data.quality.api;

public record DataTargetType(String value) {
    public static final DataTargetType TABLE = new DataTargetType("table");
    public static final DataTargetType COLUMN = new DataTargetType("column");
    public static final DataTargetType PARTITION = new DataTargetType("partition");
    public static final DataTargetType DATASET = new DataTargetType("dataset");

    public DataTargetType {
        value = QualityValues.id(value, "value");
    }
}
