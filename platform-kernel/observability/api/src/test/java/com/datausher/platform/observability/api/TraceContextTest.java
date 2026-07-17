package com.datausher.platform.observability.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceContextTest {
    @Test
    void rejectsPartialTraceContext() {
        assertThrows(IllegalArgumentException.class, () ->
                new TraceContext("trace", "", Map.of())
        );
        assertThrows(IllegalArgumentException.class, () ->
                new TraceContext("", "span", Map.of())
        );
    }

    @Test
    void snapshotsBaggage() {
        Map<String, String> baggage = new HashMap<>();
        baggage.put("tenant", "tenant-1");

        TraceContext context = new TraceContext("trace", "span", baggage);
        baggage.clear();

        assertEquals(Map.of("tenant", "tenant-1"), context.baggage());
        assertThrows(UnsupportedOperationException.class, () -> context.baggage().clear());
    }
}
