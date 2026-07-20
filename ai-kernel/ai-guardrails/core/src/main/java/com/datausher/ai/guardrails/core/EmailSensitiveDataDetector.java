package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.SensitiveDataFinding;
import com.datausher.ai.guardrails.api.SensitiveDataFilterRequest;
import com.datausher.ai.guardrails.api.SensitiveDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailSensitiveDataDetector implements SensitiveDataDetector {
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    @Override
    public List<SensitiveDataFinding> detect(SensitiveDataFilterRequest request) {
        if (!request.types().isEmpty() && !request.types().contains(SensitiveDataType.EMAIL)) {
            return List.of();
        }
        Matcher matcher = EMAIL.matcher(request.content());
        List<SensitiveDataFinding> findings = new ArrayList<>();
        while (matcher.find()) {
            findings.add(new SensitiveDataFinding(
                    SensitiveDataType.EMAIL, matcher.start(), matcher.end(), Map.of()));
        }
        return findings;
    }
}
