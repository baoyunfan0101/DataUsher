package com.datausher.development.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevelopmentLifecycleContractTest {
    @Test
    void keepsLanguagesAndWorkloadTypesOpenForExtension() {
        ScriptLanguage language = new ScriptLanguage("custom.lang");
        ExecutionSpecification specification = specification("vendor-task");

        ScriptVersion version = new ScriptVersion(
                new ScriptId("script-1"), 1, specification, Instant.EPOCH,
                "actor-1", Map.of("language", language.value()));

        assertEquals("custom.lang", version.attributes().get("language"));
        assertEquals("vendor-task", version.executionSpecification().workload().type().value());
    }

    @Test
    void requiresSubmittedDebugRunsToReferenceExecutionCore() {
        assertThrows(IllegalArgumentException.class, () -> new DebugRun(
                new DebugRunId("debug-1"), new ScriptId("script-1"), 1,
                "debug-key", Map.of(), DebugRunState.SUBMITTED, Optional.empty(),
                Instant.EPOCH, Instant.EPOCH, 1));
    }

    @Test
    void copiesMutableInputsAtContractBoundaries() {
        Map<String, ExecutionValue> parameters = new java.util.HashMap<>();
        StartDebugRunRequest request = new StartDebugRunRequest(
                new ScriptId("script-1"), 1, "debug-key", parameters, context());
        parameters.put("later", new ExecutionValue.TextValue("value"));

        assertEquals(Map.of(), request.parameters());
    }

    private static ExecutionSpecification specification(String workloadType) {
        return new ExecutionSpecification(
                new ExecutionQueueId("queue-1"), new ExecutionAccountId("account-1"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType(workloadType), "payload", Map.of(), Map.of()),
                ExecutionResultMode.REFERENCE, 100);
    }

    private static RequestContext context() {
        return RequestContext.system("request-1", Instant.EPOCH);
    }
}
