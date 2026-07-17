package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.audit.api.AuditQueryService;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.util.Objects;
import java.util.Optional;

public final class DefaultAuditService implements AuditCommandService, AuditQueryService {
    private static final IdGenerationRequest AUDIT_ID_REQUEST =
            IdGenerationRequest.of("platform-kernel", "audit-event");

    private final AuditEventStore store;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public DefaultAuditService(AuditEventStore store, IdGenerator idGenerator, Clock clock) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AuditEvent record(AuditRecordRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        AuditEvent event = new AuditEvent(
                idGenerator.nextIdValue(AUDIT_ID_REQUEST),
                request.requestContext().requestId(),
                request.requestContext().actor().actorId(),
                request.sourceModule(),
                request.action(),
                request.target(),
                request.outcome(),
                clock.now(),
                request.details()
        );
        store.append(event);
        return event;
    }

    @Override
    public Optional<AuditEvent> findById(String auditId) {
        String normalizedId = Objects.requireNonNull(auditId, "auditId must not be null").trim();
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("auditId must not be blank");
        }
        return store.findById(normalizedId);
    }

    @Override
    public PageResult<AuditEvent> search(AuditQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        return store.search(query, pageRequest);
    }
}
