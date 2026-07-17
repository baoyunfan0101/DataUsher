package com.datausher.platform.observability.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricRecorderTest {
    @Test
    void preservesActionFailureWhenTimerRecordingAlsoFails() {
        IllegalStateException actionFailure = new IllegalStateException("action failed");
        IllegalArgumentException recordingFailure = new IllegalArgumentException("recording failed");
        MetricRecorder recorder = failingTimerRecorder(recordingFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                recorder.recordDuration("operation.duration", Map.of(), () -> {
                    throw actionFailure;
                })
        );

        assertSame(actionFailure, thrown);
        assertSame(recordingFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void rejectsNullActionBeforeRecordingStarts() {
        MetricRecorder recorder = failingTimerRecorder(new IllegalStateException("must not record"));

        assertThrows(NullPointerException.class, () ->
                recorder.recordDuration("operation.duration", Map.of(), null)
        );
    }

    @Test
    void doesNotMaskFailureWhenRecorderThrowsTheSameInstance() {
        IllegalStateException sharedFailure = new IllegalStateException("shared failure");
        MetricRecorder recorder = failingTimerRecorder(sharedFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                recorder.recordDuration("operation.duration", Map.of(), () -> {
                    throw sharedFailure;
                })
        );

        assertSame(sharedFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    private static MetricRecorder failingTimerRecorder(RuntimeException failure) {
        return new MetricRecorder() {
            @Override
            public void incrementCounter(String name, Map<String, String> tags) {
            }

            @Override
            public void recordTimer(String name, Duration duration, Map<String, String> tags) {
                throw failure;
            }

            @Override
            public void recordGauge(String name, double value, Map<String, String> tags) {
            }
        };
    }
}
