package com.datausher.data.quality.api;

public record QualityCheckId(String value) {
    public QualityCheckId {
        value = QualityValues.id(value, "value");
    }
}
