package com.datausher.platform.audit.api;

public interface AuditCommandService {
    AuditEvent record(AuditRecordRequest request);
}
