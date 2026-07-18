package com.datausher.governance.access.api;

import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessContractTest {
    @Test
    void requestNormalizesActionAndCopiesEffectiveSubjects() {
        SubjectRef user = new SubjectRef(SubjectType.USER, "user-1");
        Set<SubjectRef> subjects = new HashSet<>(Set.of(user));
        AccessRequest request = new AccessRequest(
                subjects,
                "READ",
                ResourceRef.global("table", "table-1"),
                RequestContext.system("request-1", Instant.parse("2026-07-17T00:00:00Z")),
                Map.of()
        );
        subjects.add(new SubjectRef(SubjectType.GROUP, "group-1"));

        assertEquals("read", request.action());
        assertEquals(Set.of(user), request.subjects());
    }

    @Test
    void decisionRequiresBooleanAndCodeToAgree() {
        assertThrows(IllegalArgumentException.class, () -> new AccessDecision(
                true,
                AccessDecisionCode.DENIED_BY_POLICY,
                "denied",
                "policy-1",
                Instant.parse("2026-07-17T00:00:00Z")
        ));
    }

    @Test
    void subjectTypesAreOpenForExtension() {
        SubjectRef workload = new SubjectRef(new SubjectType("workload-identity"), "job-1");

        assertEquals("workload-identity:job-1", workload.canonicalValue());
    }
}
