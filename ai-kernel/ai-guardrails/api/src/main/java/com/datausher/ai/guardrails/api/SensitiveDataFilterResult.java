package com.datausher.ai.guardrails.api;

import java.util.List;
import java.util.Map;

public record SensitiveDataFilterResult(
        String filteredContent,
        List<SensitiveDataFinding> findings,
        Map<String, String> attributes
) {
    public SensitiveDataFilterResult {
        filteredContent = AiGuardrailValues.text(filteredContent, "filteredContent");
        findings = findings == null ? List.of() : List.copyOf(findings);
        attributes = AiGuardrailValues.attributes(attributes);
    }

    public boolean changed() {
        return !findings.isEmpty();
    }
}
