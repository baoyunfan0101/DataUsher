package com.datausher.platform.observability.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NoopMetricRecorderTest {
    private final NoopMetricRecorder recorder = new NoopMetricRecorder();

    @Test
    void rejectsInvalidTimerArguments() {
        assertThrows(IllegalArgumentException.class, () ->
                recorder.recordTimer("operation.duration", Duration.ofNanos(-1), Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                recorder.recordTimer("operation.duration", null, Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                recorder.recordTimer("operation.duration", Duration.ZERO, null)
        );
    }

    @Test
    void rejectsNonFiniteGaugeValues() {
        assertThrows(IllegalArgumentException.class, () ->
                recorder.recordGauge("queue.depth", Double.NaN, Map.of())
        );
        assertThrows(IllegalArgumentException.class, () ->
                recorder.recordGauge("queue.depth", Double.POSITIVE_INFINITY, Map.of())
        );
    }

    @Test
    void rejectsInvalidTags() {
        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("module", null);

        assertThrows(IllegalArgumentException.class, () ->
                recorder.incrementCounter("operation.count", Map.of(" ", "value"))
        );
        assertThrows(NullPointerException.class, () ->
                recorder.incrementCounter("operation.count", nullValue)
        );
    }
}
