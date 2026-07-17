package com.datausher.platform.audit.api;

public interface AuditedCommand<T> {
    T execute();

    AuditRecordRequest audit(T result);

    void rollback(T result, RuntimeException cause);
}
