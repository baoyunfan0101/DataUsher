package com.datausher.data.quality.api;

public record QualitySeverity(String value) {
    public static final QualitySeverity INFO = new QualitySeverity("info");
    public static final QualitySeverity WARNING = new QualitySeverity("warning");
    public static final QualitySeverity ERROR = new QualitySeverity("error");
    public static final QualitySeverity CRITICAL = new QualitySeverity("critical");

    public QualitySeverity {
        value = QualityValues.id(value, "value");
    }
}
