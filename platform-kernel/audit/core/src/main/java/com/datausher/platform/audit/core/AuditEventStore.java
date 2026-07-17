package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditEvent;
import com.datausher.platform.audit.api.AuditQuery;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface AuditEventStore {
    void append(AuditEvent event);

    Optional<AuditEvent> findById(String auditId);

    PageResult<AuditEvent> search(AuditQuery query, PageRequest pageRequest);
}
