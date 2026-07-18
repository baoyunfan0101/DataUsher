package com.datausher.execution.api;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionContractTest {
    @Test
    void preservesCustomWorkloadAndResultTypes() {
        var workload = new ExecutionWorkload(
                new ExecutionWorkloadType("python.batch"),
                "artifact://jobs/report.py",
                Map.of("date", new ExecutionValue.TextValue("2026-07-18")),
                Map.of()
        );
        var request = new SubmitExecutionRequest(
                new ExecutionSpecification(
                        new ExecutionQueueId("Default"),
                        new ExecutionAccountId("Spark-Prod"),
                        workload,
                        new ExecutionResultMode("materialized-reference"),
                        100),
                "report-2026-07-18",
                ExecutionOrigin.direct("request-1"),
                RequestContext.system("request-1", Instant.EPOCH)
        );

        assertEquals("python.batch", request.specification().workload().type().value());
        assertEquals("materialized-reference", request.specification().resultMode().value());
        assertEquals("default", request.specification().queueId().value());
    }

    @Test
    void requiresTerminalInstancesToHaveFinishTime() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutionInstance(
                new ExecutionInstanceId("instance-1"),
                new ExecutionRequestId("request-1"),
                1,
                ExecutionState.SUCCEEDED,
                Instant.EPOCH,
                Optional.of(Instant.EPOCH),
                Optional.empty(),
                Optional.empty(),
                1
        ));
        assertTrue(ExecutionState.TIMED_OUT.terminal());
    }
}
