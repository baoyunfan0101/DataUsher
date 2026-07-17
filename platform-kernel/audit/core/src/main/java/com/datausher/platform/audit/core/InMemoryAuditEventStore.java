package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAuditEventStore implements AuditEventStore {
    private final ConcurrentMap<String, AuditEvent> events = new ConcurrentHashMap<>();

    @Override
    public void append(AuditEvent event) {
        AuditEvent existing = events.putIfAbsent(event.auditId(), event);
        if (existing != null) {
            throw new IllegalStateException("audit event already exists: " + event.auditId());
        }
    }

    @Override
    public Optional<AuditEvent> findById(String auditId) {
        return Optional.ofNullable(events.get(auditId));
    }

    @Override
    public PageResult<AuditEvent> search(AuditQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<AuditEvent> matches = new ArrayList<>();
        for (AuditEvent event : events.values()) {
            if (matches(event, query)) {
                matches.add(event);
            }
        }
        matches.sort(Comparator.comparing(AuditEvent::occurredAt).reversed()
                .thenComparing(AuditEvent::auditId));
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = (int) Math.min(
                (long) fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    private static boolean matches(AuditEvent event, AuditQuery query) {
        return (query.actorId() == null || query.actorId().equals(event.actorId()))
                && (query.sourceModule() == null || query.sourceModule().equals(event.sourceModule()))
                && (query.action() == null || query.action().equals(event.action()))
                && (query.target() == null || query.target().equals(event.target()))
                && (query.outcome() == null || query.outcome() == event.outcome())
                && (query.occurredFrom() == null || !event.occurredAt().isBefore(query.occurredFrom()))
                && (query.occurredTo() == null || !event.occurredAt().isAfter(query.occurredTo()));
    }
}
