package com.datausher.data.quality.api;

public record QualityRuleId(String value) {
    public QualityRuleId {
        value = QualityValues.id(value, "value");
    }
}
