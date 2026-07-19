package com.datausher.data.quality.api;

public record ProfileMetricType(String value) {
    public static final ProfileMetricType ROW_COUNT = new ProfileMetricType("row-count");
    public static final ProfileMetricType NULL_COUNT = new ProfileMetricType("null-count");
    public static final ProfileMetricType NULL_RATIO = new ProfileMetricType("null-ratio");
    public static final ProfileMetricType DISTINCT_COUNT = new ProfileMetricType("distinct-count");
    public static final ProfileMetricType MINIMUM = new ProfileMetricType("minimum");
    public static final ProfileMetricType MAXIMUM = new ProfileMetricType("maximum");
    public static final ProfileMetricType DISTRIBUTION = new ProfileMetricType("distribution");

    public ProfileMetricType {
        value = QualityValues.id(value, "value");
    }
}
