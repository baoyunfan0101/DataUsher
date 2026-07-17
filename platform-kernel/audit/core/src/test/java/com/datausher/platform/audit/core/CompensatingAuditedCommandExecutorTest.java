package com.datausher.platform.audit.core;

import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompensatingAuditedCommandExecutorTest {
    @Test
    void rollsBackCompletedMutationWhenAuditFails() {
        AuditCommandService failingAudit = request -> {
            throw new IllegalStateException("audit unavailable");
        };
        CompensatingAuditedCommandExecutor executor =
                new CompensatingAuditedCommandExecutor(failingAudit);
        AtomicBoolean mutated = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> executor.execute(new AuditedCommand<String>() {
            @Override
            public String execute() {
                mutated.set(true);
                return "result";
            }

            @Override
            public AuditRecordRequest audit(String result) {
                return new AuditRecordRequest(
                        RequestContext.system("request-1", Instant.parse("2026-07-17T00:00:00Z")),
                        "test-module",
                        "test.execute",
                        AuditTarget.global("test", result),
                        AuditOutcome.SUCCEEDED,
                        Map.of()
                );
            }

            @Override
            public void rollback(String result, RuntimeException cause) {
                mutated.set(false);
            }
        }));

        assertTrue(!mutated.get());
    }
}
