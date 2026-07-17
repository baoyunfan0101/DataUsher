package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;

import java.util.Objects;

public final class CompensatingAuditedCommandExecutor implements AuditedCommandExecutor {
    private final AuditCommandService auditService;

    public CompensatingAuditedCommandExecutor(AuditCommandService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
    }

    @Override
    public <T> T execute(AuditedCommand<T> command) {
        Objects.requireNonNull(command, "command must not be null");
        T result = command.execute();
        try {
            auditService.record(Objects.requireNonNull(
                    command.audit(result), "audit request must not be null"));
            return result;
        } catch (RuntimeException auditFailure) {
            try {
                command.rollback(result, auditFailure);
            } catch (RuntimeException rollbackFailure) {
                auditFailure.addSuppressed(rollbackFailure);
            }
            throw auditFailure;
        }
    }
}
