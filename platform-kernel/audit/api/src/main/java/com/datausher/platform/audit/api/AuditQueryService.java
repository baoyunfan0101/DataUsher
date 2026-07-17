package com.datausher.platform.audit.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface AuditQueryService {
    Optional<AuditEvent> findById(String auditId);

    PageResult<AuditEvent> search(AuditQuery query, PageRequest pageRequest);
}
