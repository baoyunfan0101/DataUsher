package com.datausher.platform.observability.core;

import com.datausher.platform.observability.api.MetricRecorder;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class NoopMetricRecorder implements MetricRecorder {
    @Override
    public void incrementCounter(String name, Map<String, String> tags) {
        validateName(name);
        validateTags(tags);
    }

    @Override
    public void recordTimer(String name, Duration duration, Map<String, String> tags) {
        validateName(name);
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        validateTags(tags);
    }

    @Override
    public void recordGauge(String name, double value, Map<String, String> tags) {
        validateName(name);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("gauge value must be finite");
        }
        validateTags(tags);
    }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "metric name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("metric name must not be blank");
        }
    }

    private static void validateTags(Map<String, String> tags) {
        Objects.requireNonNull(tags, "metric tags must not be null");
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            String tagName = Objects.requireNonNull(tag.getKey(), "metric tag name must not be null");
            if (tagName.isBlank()) {
                throw new IllegalArgumentException("metric tag name must not be blank");
            }
            Objects.requireNonNull(tag.getValue(), "metric tag value must not be null");
        }
    }
}
