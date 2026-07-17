package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.shared.page.PageRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryAuditEventStoreTest {
    @Test
    void appliesFilteringOrderingAndPaginationInsideTheStore() {
        InMemoryAuditEventStore store = new InMemoryAuditEventStore();
        store.append(event("audit-1", "2026-07-17T00:00:00Z", "catalog"));
        store.append(event("audit-2", "2026-07-17T00:01:00Z", "catalog"));
        store.append(event("audit-3", "2026-07-17T00:02:00Z", "scheduler"));

        var result = store.search(
                new AuditQuery(null, "catalog", null, null, null, null, null),
                new PageRequest(2, 1, List.of())
        );

        assertEquals(2, result.total());
        assertEquals(List.of("audit-1"), result.items().stream()
                .map(AuditEvent::auditId)
                .toList());
    }

    private static AuditEvent event(String auditId, String occurredAt, String sourceModule) {
        return new AuditEvent(
                auditId,
                "request-1",
                "actor-1",
                sourceModule,
                "resource.read",
                AuditTarget.global("resource", "resource-1"),
                AuditOutcome.SUCCEEDED,
                Instant.parse(occurredAt),
                Map.of()
        );
    }
}
