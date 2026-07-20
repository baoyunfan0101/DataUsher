package com.datausher.ai.guardrails.api;

public interface SensitiveDataFilter {
    SensitiveDataFilterResult filter(SensitiveDataFilterRequest request);
}
