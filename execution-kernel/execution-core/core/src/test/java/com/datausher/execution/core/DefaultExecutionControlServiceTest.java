package com.datausher.execution.core;

import com.datausher.execution.api.ChangeExecutionAccountStatusRequest;
import com.datausher.execution.api.ChangeExecutionQueueStatusRequest;
import com.datausher.execution.api.CreateExecutionQueueRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionAccountStatus;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionQueueStatus;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.RegisterExecutionAccountRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultExecutionControlServiceTest {
    @Test
    void managesQueuesAndAccountsWithOptimisticRevisions() {
        MutableClock clock = new MutableClock();
        var service = new DefaultExecutionControlService(
                new InMemoryExecutionQueueStore(),
                new InMemoryExecutionAccountStore(),
                clock
        );
        var context = RequestContext.system("request-1", clock.now());
        var queue = service.create(new CreateExecutionQueueRequest(
                new ExecutionQueueId("batch"), "Batch", 4, 10, Map.of(), context));
        var account = service.register(new RegisterExecutionAccountRequest(
                new ExecutionAccountId("local"), "Local", "local-compute", "local-binding",
                Set.of(new ExecutionWorkloadType("fixture")), Map.of(), context));

        clock.advance();
        var disabledQueue = service.changeStatus(new ChangeExecutionQueueStatusRequest(
                queue.queueId(), ExecutionQueueStatus.DISABLED, queue.revision(), context));
        var disabledAccount = service.changeStatus(new ChangeExecutionAccountStatusRequest(
                account.accountId(), ExecutionAccountStatus.DISABLED,
                account.revision(), context));

        assertEquals(2, disabledQueue.revision());
        assertEquals(2, disabledAccount.revision());
        assertFalse(disabledAccount.supports(new ExecutionWorkloadType("python")));
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(new ChangeExecutionQueueStatusRequest(
                        queue.queueId(), ExecutionQueueStatus.ACTIVE, queue.revision(), context)));
    }

    @Test
    void emptyWorkloadSetMeansAllCurrentAndFutureTypes() {
        MutableClock clock = new MutableClock();
        var service = new DefaultExecutionControlService(
                new InMemoryExecutionQueueStore(),
                new InMemoryExecutionAccountStore(),
                clock
        );
        var context = RequestContext.system("request-1", clock.now());
        var account = service.register(new RegisterExecutionAccountRequest(
                new ExecutionAccountId("generic"), "Generic", "generic-compute",
                "generic-binding", Set.of(), Map.of(), context));

        assertTrue(account.supports(new ExecutionWorkloadType("future-language")));
    }

    private static final class MutableClock implements Clock {
        private Instant now = Instant.EPOCH;

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public ZoneId zone() {
            return ZoneOffset.UTC;
        }

        void advance() {
            now = now.plusSeconds(1);
        }
    }
}
