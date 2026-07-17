package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAuditServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void recordsAndQueriesImmutableAuditFacts() {
        DefaultAuditService service = new DefaultAuditService(
                new InMemoryAuditEventStore(), new UuidIdGenerator(), fixedClock());
        var event = service.record(new AuditRecordRequest(
                RequestContext.system("request-1", NOW),
                "resource-registry",
                "resource.register",
                AuditTarget.global("project", "project-1"),
                AuditOutcome.SUCCEEDED,
                Map.of("key", "analytics")
        ));

        var result = service.search(
                new AuditQuery(null, "resource-registry", null, null, null, null, null),
                PageRequest.firstPage()
        );

        assertEquals(1, result.total());
        assertEquals(event, result.items().get(0));
        assertTrue(service.findById(event.auditId()).isPresent());
    }

    private static Clock fixedClock() {
        return new Clock() {
            @Override
            public Instant now() {
                return NOW;
            }

            @Override
            public ZoneId zone() {
                return ZoneOffset.UTC;
            }
        };
    }
}
