package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditQuery;

import java.util.List;
import java.util.Optional;

public interface AuditEventStore {
    void append(AuditEvent event);

    Optional<AuditEvent> findById(String auditId);

    List<AuditEvent> find(AuditQuery query);
}
