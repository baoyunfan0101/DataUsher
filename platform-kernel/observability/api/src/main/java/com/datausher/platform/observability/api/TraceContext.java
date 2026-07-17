package com.datausher.platform.observability.api;

import java.util.Map;

public record TraceContext(String traceId, String spanId, Map<String, String> baggage) {
    public static final TraceContext EMPTY = new TraceContext("", "", Map.of());

    public TraceContext {
        traceId = traceId == null ? "" : traceId.trim();
        spanId = spanId == null ? "" : spanId.trim();
        baggage = baggage == null ? Map.of() : Map.copyOf(baggage);
        if (traceId.isEmpty() != spanId.isEmpty()) {
            throw new IllegalArgumentException("traceId and spanId must either both be present or both be empty");
        }
    }

    public boolean present() {
        return !traceId.isEmpty();
    }
}
