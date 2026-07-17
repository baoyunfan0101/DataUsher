package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditQuery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    public List<AuditEvent> find(AuditQuery query) {
        List<AuditEvent> matches = new ArrayList<>();
        for (AuditEvent event : events.values()) {
            if (matches(event, query)) {
                matches.add(event);
            }
        }
        matches.sort(Comparator.comparing(AuditEvent::occurredAt).reversed()
                .thenComparing(AuditEvent::auditId));
        return List.copyOf(matches);
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
