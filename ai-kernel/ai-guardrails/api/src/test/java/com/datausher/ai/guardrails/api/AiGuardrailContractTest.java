package com.datausher.ai.guardrails.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGuardrailContractTest {
    @Test
    void keepsFindingAndSensitiveTypesOpenForExtension() {
        assertEquals("tenant-policy", new AiGuardrailFindingType("tenant-policy").value());
        assertEquals("tax-id", new SensitiveDataType("tax-id").value());
    }

    @Test
    void requiresSubjectsForPermissionReview() {
        assertThrows(IllegalArgumentException.class, () -> new AiGuardrailRequest(
                Set.of(), "read", ResourceRef.global("table", "orders"),
                Map.of(), context()));
    }

    @Test
    void validatesSensitiveDataRanges() {
        assertThrows(IllegalArgumentException.class, () -> new SensitiveDataFinding(
                SensitiveDataType.EMAIL, 5, 5, Map.of()));
    }

    static SubjectRef subject() {
        return new SubjectRef(SubjectType.USER, "alice");
    }

    static RequestContext context() {
        return RequestContext.system("request-guardrail", Instant.EPOCH);
    }
}
