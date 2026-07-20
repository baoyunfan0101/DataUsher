package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.SensitiveDataFilter;
import com.datausher.ai.guardrails.api.SensitiveDataFilterRequest;
import com.datausher.ai.guardrails.api.SensitiveDataFilterResult;
import com.datausher.ai.guardrails.api.SensitiveDataFinding;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultSensitiveDataFilter implements SensitiveDataFilter {
    private final List<SensitiveDataDetector> detectors;

    public DefaultSensitiveDataFilter(List<SensitiveDataDetector> detectors) {
        this.detectors = List.copyOf(Objects.requireNonNull(
                detectors, "detectors must not be null"));
    }

    @Override
    public SensitiveDataFilterResult filter(SensitiveDataFilterRequest request) {
        List<SensitiveDataFinding> findings = detectors.stream()
                .flatMap(detector -> detector.detect(request).stream())
                .sorted(Comparator.comparingInt(SensitiveDataFinding::startInclusive))
                .toList();
        if (findings.isEmpty()) {
            return new SensitiveDataFilterResult(request.content(), List.of(), Map.of());
        }
        StringBuilder filtered = new StringBuilder();
        int cursor = 0;
        for (SensitiveDataFinding finding : findings) {
            if (finding.startInclusive() < cursor) {
                continue;
            }
            filtered.append(request.content(), cursor, finding.startInclusive());
            filtered.append(request.replacement());
            cursor = finding.endExclusive();
        }
        filtered.append(request.content().substring(cursor));
        return new SensitiveDataFilterResult(filtered.toString(), findings, Map.of());
    }
}
