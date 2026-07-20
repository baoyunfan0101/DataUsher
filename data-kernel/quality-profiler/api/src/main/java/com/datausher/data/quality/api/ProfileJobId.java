package com.datausher.data.quality.api;

public record ProfileJobId(String value) {
    public ProfileJobId {
        value = QualityValues.id(value, "value");
    }
}
