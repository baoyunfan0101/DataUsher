package com.datausher.platform.observability.api;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public interface MetricRecorder {
    void incrementCounter(String name, Map<String, String> tags);

    void recordTimer(String name, Duration duration, Map<String, String> tags);

    void recordGauge(String name, double value, Map<String, String> tags);

    default <T> T recordDuration(String name, Map<String, String> tags, Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        long startedAt = System.nanoTime();
        Throwable actionFailure = null;
        try {
            return action.get();
        } catch (RuntimeException | Error failure) {
            actionFailure = failure;
            throw failure;
        } finally {
            try {
                recordTimer(name, Duration.ofNanos(System.nanoTime() - startedAt), tags);
            } catch (RuntimeException | Error recordingFailure) {
                if (actionFailure != null) {
                    if (actionFailure != recordingFailure) {
                        actionFailure.addSuppressed(recordingFailure);
                    }
                } else {
                    throw recordingFailure;
                }
            }
        }
    }
}
