package com.datausher.governance.access.core;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryAccessPolicyStoreTest {
    @Test
    void ordersByPriorityAndPrefersDenyAtEqualPriority() {
        InMemoryAccessPolicyStore store = new InMemoryAccessPolicyStore();
        SubjectRef subject = new SubjectRef(SubjectType.USER, "user-1");
        ResourceRef resource = ResourceRef.global("table", "table-1");
        store.save(policy("low-deny", subject, PolicyEffect.DENY, 10));
        store.save(policy("high-allow", subject, PolicyEffect.ALLOW, 100));
        store.save(policy("high-deny", subject, PolicyEffect.DENY, 100));

        assertEquals("high-deny", store.findEffective(Set.of(subject), "read", resource)
                .orElseThrow()
                .policyId());
    }

    private static AccessPolicy policy(
            String policyId,
            SubjectRef subject,
            PolicyEffect effect,
            int priority
    ) {
        return new AccessPolicy(
                policyId,
                subject,
                "table",
                "read",
                ResourceScope.global(),
                effect,
                priority,
                true
        );
    }
}
