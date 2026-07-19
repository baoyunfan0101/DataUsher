package com.datausher.data.quality.api;

public record QualityRuleType(String value) {
    public static final QualityRuleType NOT_NULL = new QualityRuleType("not-null");
    public static final QualityRuleType UNIQUE = new QualityRuleType("unique");
    public static final QualityRuleType ROW_COUNT = new QualityRuleType("row-count");
    public static final QualityRuleType ACCEPTED_VALUES = new QualityRuleType("accepted-values");
    public static final QualityRuleType EXPRESSION = new QualityRuleType("expression");

    public QualityRuleType {
        value = QualityValues.id(value, "value");
    }
}
