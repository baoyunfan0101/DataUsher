package com.datausher.data.quality.core;

import com.datausher.data.quality.api.CreateQualityRuleRequest;
import com.datausher.data.quality.api.CreateQualityRuleVersionRequest;
import com.datausher.data.quality.api.DataExecutionPolicy;
import com.datausher.data.quality.api.DataTargetRef;
import com.datausher.data.quality.api.DataTargetType;
import com.datausher.data.quality.api.QualityCheckState;
import com.datausher.data.quality.api.QualityOutcome;
import com.datausher.data.quality.api.QualityResult;
import com.datausher.data.quality.api.QualityRuleId;
import com.datausher.data.quality.api.QualityRuleRef;
import com.datausher.data.quality.api.QualityRuleSpec;
import com.datausher.data.quality.api.QualityRuleType;
import com.datausher.data.quality.api.QualitySeverity;
import com.datausher.data.quality.api.StartQualityCheckRequest;
import com.datausher.execution.api.CancelExecutionRequest;
import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionCommandService;
import com.datausher.execution.api.ExecutionInstance;
import com.datausher.execution.api.ExecutionInstanceId;
import com.datausher.execution.api.ExecutionQuery;
import com.datausher.execution.api.ExecutionQueryService;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionRequest;
import com.datausher.execution.api.ExecutionRequestId;
import com.datausher.execution.api.ExecutionState;
import com.datausher.execution.api.ExecutionStateChangedEvent;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultQualityServicesTest {
    private static final ExecutionWorkloadType WORKLOAD_TYPE =
            new ExecutionWorkloadType("quality-fixture");

    @Test
    void versionsRulesAndEvaluatesPinnedVersionsThroughExecution() {
        var ruleService = new DefaultQualityRuleService(
                new InMemoryQualityRuleStore(), new SystemClock());
        RequestContext context = RequestContext.system("request-1", Instant.now());
        QualityRuleId ruleId = new QualityRuleId("orders-not-null");
        var rule = ruleService.create(new CreateQualityRuleRequest(
                ruleId, ruleSpec("v1"), Map.of(), context));
        var version = ruleService.createVersion(new CreateQualityRuleVersionRequest(
                ruleId, rule.revision(), ruleSpec("v2"), context));
        assertThrows(RevisionConflictException.class,
                () -> ruleService.createVersion(new CreateQualityRuleVersionRequest(
                        ruleId, rule.revision(), ruleSpec("stale"), context)));

        RecordingExecutions executions = new RecordingExecutions();
        QualityExecutionPlanner planner = new QualityExecutionPlanner() {
            @Override
            public com.datausher.data.quality.api.AssessmentExecutionType executionType() {
                return com.datausher.data.quality.api.AssessmentExecutionType.COMPUTE;
            }

            @Override
            public ExecutionWorkload plan(QualityCheckPlanningRequest request) {
                return new ExecutionWorkload(
                        WORKLOAD_TYPE, "quality-check", Map.of(), Map.of());
            }
        };
        QualityResultDecoder decoder = new QualityResultDecoder() {
            @Override
            public ExecutionWorkloadType workloadType() {
                return WORKLOAD_TYPE;
            }

            @Override
            public List<QualityResult> decode(QualityResultDecodingRequest request) {
                return request.rules().stream().map(ruleVersion -> new QualityResult(
                        request.check().checkId(),
                        new QualityRuleRef(ruleVersion.ruleId(), ruleVersion.version()),
                        QualityOutcome.FAILED, ruleVersion.specification().severity(),
                        Optional.empty(), "null values found", Map.of(), Instant.now())).toList();
            }
        };
        var checks = new DefaultQualityCheckService(
                new InMemoryQualityCheckStore(), ruleService, executions, executions,
                new QualityExecutionPlannerRegistry(List.of(planner)),
                new QualityResultDecoderRegistry(List.of(decoder)),
                new UuidIdGenerator(), new SystemClock());
        StartQualityCheckRequest request = new StartQualityCheckRequest(
                List.of(new QualityRuleRef(ruleId, version.version())), policy(),
                "quality-key", Map.of(), context);

        var first = checks.start(request);
        var duplicate = checks.start(request);
        var submitted = checks.dispatch(first.checkId(), context);
        checks.handleExecutionStateChanged(completed(
                executions.requests.get(submitted.executionRequestId().orElseThrow()), context));

        assertEquals(first.checkId(), duplicate.checkId());
        assertEquals(QualityCheckState.SUCCEEDED,
                checks.findCheck(first.checkId()).orElseThrow().state());
        assertEquals(QualityOutcome.FAILED,
                checks.listResults(first.checkId()).getFirst().outcome());
    }

    private static QualityRuleSpec ruleSpec(String version) {
        return new QualityRuleSpec(
                "Orders not null " + version,
                new DataTargetRef(DataTargetType.COLUMN, "orders.customer_id", Map.of()),
                QualityRuleType.NOT_NULL, Map.of(), QualitySeverity.ERROR, Map.of());
    }

    private static DataExecutionPolicy policy() {
        return new DataExecutionPolicy(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                100, Map.of());
    }

    private static ExecutionStateChangedEvent completed(
            ExecutionRequest request,
            RequestContext context
    ) {
        Instant now = Instant.now();
        ExecutionRequest completed = new ExecutionRequest(
                request.requestId(), request.specification(), request.idempotencyKey(),
                request.origin(), ExecutionState.SUCCEEDED, request.submittedAt(), now,
                Optional.empty(), request.revision() + 1);
        return new ExecutionStateChangedEvent(
                "event-quality", now, context, completed, Optional.empty());
    }

    private static final class RecordingExecutions
            implements ExecutionCommandService, ExecutionQueryService {
        private final Map<ExecutionRequestId, ExecutionRequest> requests = new HashMap<>();

        @Override
        public ExecutionRequest submit(SubmitExecutionRequest request) {
            return requests.values().stream()
                    .filter(existing -> existing.idempotencyKey().equals(request.idempotencyKey()))
                    .findFirst().orElseGet(() -> {
                        Instant now = Instant.now();
                        ExecutionRequest created = new ExecutionRequest(
                                new ExecutionRequestId("execution-" + (requests.size() + 1)),
                                request.specification(), request.idempotencyKey(), request.origin(),
                                ExecutionState.QUEUED, now, now, Optional.empty(), 1);
                        requests.put(created.requestId(), created);
                        return created;
                    });
        }

        @Override
        public ExecutionRequest cancel(CancelExecutionRequest request) {
            ExecutionRequest current = requests.get(request.requestId());
            Instant now = Instant.now();
            ExecutionRequest cancelled = new ExecutionRequest(
                    current.requestId(), current.specification(), current.idempotencyKey(),
                    current.origin(), ExecutionState.CANCELLED, current.submittedAt(), now,
                    Optional.empty(), current.revision() + 1);
            requests.put(cancelled.requestId(), cancelled);
            return cancelled;
        }

        @Override
        public Optional<ExecutionRequest> findRequest(ExecutionRequestId requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public Optional<ExecutionInstance> findInstance(ExecutionInstanceId instanceId) {
            return Optional.empty();
        }

        @Override
        public List<ExecutionInstance> listInstances(ExecutionRequestId requestId) {
            return List.of();
        }

        @Override
        public PageResult<ExecutionRequest> search(
                ExecutionQuery query,
                PageRequest pageRequest
        ) {
            List<ExecutionRequest> values = List.copyOf(requests.values());
            return new PageResult<>(values, values.size(), pageRequest.page(), pageRequest.size());
        }
    }
}
