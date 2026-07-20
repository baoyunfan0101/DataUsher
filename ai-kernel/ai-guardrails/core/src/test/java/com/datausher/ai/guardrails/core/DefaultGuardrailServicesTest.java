package com.datausher.ai.guardrails.core;

import com.datausher.ai.guardrails.api.AiGuardrailDecisionCode;
import com.datausher.ai.guardrails.api.AiGuardrailRequest;
import com.datausher.ai.guardrails.api.SensitiveDataFilterRequest;
import com.datausher.ai.guardrails.api.SensitiveDataType;
import com.datausher.ai.guardrails.api.SqlSafetyReviewRequest;
import com.datausher.governance.access.api.AccessDecision;
import com.datausher.governance.access.api.AccessDecisionCode;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultGuardrailServicesTest {
    @Test
    void permissionGuardrailDelegatesToAccessDecisions() {
        DefaultAiGuardrailService service = new DefaultAiGuardrailService(
                request -> new AccessDecision(false, AccessDecisionCode.DENIED_BY_POLICY,
                        "denied", null, Instant.EPOCH), clock());

        var review = service.review(new AiGuardrailRequest(
                Set.of(subject()), "read", ResourceRef.global("table", "orders"),
                Map.of(), context()));

        assertFalse(review.allowed());
        assertEquals(AiGuardrailDecisionCode.DENIED_BY_PERMISSION, review.code());
    }

    @Test
    void sqlSafetyAllowsReadsAndBlocksMutations() {
        DefaultSqlSafetyReviewService service = new DefaultSqlSafetyReviewService(
                List.of(new ReadOnlySqlSafetyRule()), clock());

        assertTrue(service.review(new SqlSafetyReviewRequest(
                "select * from orders", Map.of(), Map.of(), context())).allowed());
        assertEquals(AiGuardrailDecisionCode.BLOCKED_BY_SQL_SAFETY,
                service.review(new SqlSafetyReviewRequest(
                        "delete from orders", Map.of(), Map.of(), context())).code());
    }

    @Test
    void sensitiveDataFilterRedactsDetectedValues() {
        DefaultSensitiveDataFilter filter = new DefaultSensitiveDataFilter(
                List.of(new EmailSensitiveDataDetector()));

        var result = filter.filter(new SensitiveDataFilterRequest(
                "contact alice@example.com", Set.of(SensitiveDataType.EMAIL),
                "[email]", Map.of(), context()));

        assertTrue(result.changed());
        assertEquals("contact [email]", result.filteredContent());
    }

    private static SubjectRef subject() {
        return new SubjectRef(SubjectType.USER, "alice");
    }

    private static RequestContext context() {
        return RequestContext.system("request-guardrail", Instant.EPOCH);
    }

    private static Clock clock() {
        return new Clock() {
            @Override
            public Instant now() {
                return Instant.EPOCH;
            }

            @Override
            public ZoneId zone() {
                return ZoneId.of("UTC");
            }
        };
    }
}
