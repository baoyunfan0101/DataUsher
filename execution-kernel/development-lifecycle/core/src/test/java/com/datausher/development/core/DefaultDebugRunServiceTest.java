package com.datausher.development.core;

import com.datausher.development.api.DebugRunState;
import com.datausher.development.api.ScriptDefinition;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptQueryService;
import com.datausher.development.api.ScriptVersion;
import com.datausher.development.api.StartDebugRunRequest;
import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDebugRunServiceTest {
    @Test
    void persistsBeforeDispatchAndDelegatesExecutionLifecycle() {
        ScriptId scriptId = new ScriptId("script-1");
        ScriptVersion version = new ScriptVersion(
                scriptId, 1, specification(), Instant.EPOCH, "actor-1", Map.of());
        RecordingExecutions executions = new RecordingExecutions();
        var service = new DefaultDebugRunService(
                scripts(version), new InMemoryDebugRunStore(), executions,
                new UuidIdGenerator(), new SystemClock());
        RequestContext context = RequestContext.system("request-1", Instant.now());
        StartDebugRunRequest request = new StartDebugRunRequest(
                scriptId, 1, "debug-key",
                Map.of("limit", new ExecutionValue.DecimalValue(10)), context);

        var pending = service.start(request);

        assertEquals(DebugRunState.PENDING, pending.state());
        assertTrue(pending.executionRequestId().isEmpty());
        assertEquals(List.of(), executions.submissions);

        var submitted = service.dispatch(pending.debugRunId(), context);
        var repeated = service.dispatch(pending.debugRunId(), context);

        assertEquals(DebugRunState.SUBMITTED, submitted.state());
        assertEquals(submitted, repeated);
        assertEquals(1, executions.submissions.size());
        assertEquals(new ExecutionValue.DecimalValue(10),
                executions.submissions.getFirst().specification().workload().parameters().get("limit"));
        assertEquals("debug-run", executions.submissions.getFirst().origin().type().value());
    }

    @Test
    void protectsDebugIdempotencyKeysFromSemanticReuse() {
        ScriptId scriptId = new ScriptId("script-1");
        ScriptVersion version = new ScriptVersion(
                scriptId, 1, specification(), Instant.EPOCH, "actor-1", Map.of());
        var service = new DefaultDebugRunService(
                scripts(version), new InMemoryDebugRunStore(), new RecordingExecutions(),
                new UuidIdGenerator(), new SystemClock());
        RequestContext context = RequestContext.system("request-1", Instant.now());
        var original = service.start(new StartDebugRunRequest(
                scriptId, 1, "debug-key", Map.of(), context));

        var repeated = service.start(new StartDebugRunRequest(
                scriptId, 1, "debug-key", Map.of(), context));

        assertEquals(original, repeated);
        assertThrows(IllegalStateException.class, () -> service.start(new StartDebugRunRequest(
                scriptId, 1, "debug-key",
                Map.of("different", new ExecutionValue.BooleanValue(true)), context)));
    }

    private static ScriptQueryService scripts(ScriptVersion version) {
        return new ScriptQueryService() {
            @Override
            public Optional<ScriptDefinition> findScript(ScriptId scriptId) {
                return Optional.empty();
            }

            @Override
            public Optional<ScriptVersion> findVersion(ScriptId scriptId, long candidateVersion) {
                return version.scriptId().equals(scriptId) && version.version() == candidateVersion
                        ? Optional.of(version) : Optional.empty();
            }

            @Override
            public Optional<ScriptVersion> findLatestVersion(ScriptId scriptId) {
                return version.scriptId().equals(scriptId) ? Optional.of(version) : Optional.empty();
            }

            @Override
            public List<ScriptVersion> listVersions(ScriptId scriptId) {
                return version.scriptId().equals(scriptId) ? List.of(version) : List.of();
            }
        };
    }

    private static ExecutionSpecification specification() {
        return new ExecutionSpecification(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType("vendor-task"), "payload",
                        Map.of("base", new ExecutionValue.TextValue("value")), Map.of()),
                ExecutionResultMode.REFERENCE, 100);
    }

    private static final class RecordingExecutions implements ExecutionCommandService {
        private final java.util.ArrayList<SubmitExecutionRequest> submissions = new java.util.ArrayList<>();

        @Override
        public ExecutionRequest submit(SubmitExecutionRequest request) {
            submissions.add(request);
            Instant now = Instant.now();
            return new ExecutionRequest(
                    new ExecutionRequestId("execution-" + submissions.size()), request.specification(),
                    request.idempotencyKey(), request.origin(), ExecutionState.PENDING,
                    now, now, Optional.empty(), 1);
        }

        @Override
        public ExecutionRequest cancel(CancelExecutionRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
