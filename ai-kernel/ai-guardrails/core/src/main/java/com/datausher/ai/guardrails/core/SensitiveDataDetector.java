package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.SensitiveDataFinding;
import com.datausher.ai.guardrails.api.SensitiveDataFilterRequest;

import java.util.List;

public interface SensitiveDataDetector {
    List<SensitiveDataFinding> detect(SensitiveDataFilterRequest request);
}
