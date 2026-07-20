package com.datausher.data.quality.api;

public record AssessmentExecutionType(String value) {
    public static final AssessmentExecutionType COMPUTE = new AssessmentExecutionType("compute");

    public AssessmentExecutionType {
        value = QualityValues.id(value, "value");
    }
}
