package com.datausher.ai.context.api;

public record AiContextSourceType(String value) {
    public static final AiContextSourceType METADATA = new AiContextSourceType("metadata");
    public static final AiContextSourceType LINEAGE = new AiContextSourceType("lineage");
    public static final AiContextSourceType QUERY = new AiContextSourceType("query");
    public static final AiContextSourceType LOG = new AiContextSourceType("log");
    public static final AiContextSourceType QUALITY = new AiContextSourceType("quality");

    public AiContextSourceType {
        value = AiContextValues.id(value, "value");
    }
}
