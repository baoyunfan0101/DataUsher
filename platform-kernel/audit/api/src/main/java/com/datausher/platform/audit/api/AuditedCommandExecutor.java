package com.datausher.platform.audit.api;

public interface AuditedCommandExecutor {
    <T> T execute(AuditedCommand<T> command);
}
