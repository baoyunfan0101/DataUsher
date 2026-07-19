package com.datausher.app;

import com.datausher.data.lineage.api.ApplyLineageSnapshotRequest;
import com.datausher.data.lineage.api.ImpactAnalysisRequest;
import com.datausher.data.lineage.api.LineageEdgeInput;
import com.datausher.data.lineage.api.LineageEdgeType;
import com.datausher.data.lineage.api.LineageNodeInput;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageNodeType;
import com.datausher.data.lineage.api.LineageSnapshotMode;
import com.datausher.data.lineage.api.LineageSourceRef;
import com.datausher.data.lineage.api.LineageSourceType;
import com.datausher.data.lineage.core.DefaultLineageService;
import com.datausher.data.lineage.core.InMemoryLineageStore;
import com.datausher.data.lineage.core.Sha256LineageIdFactory;
import com.datausher.data.quality.api.AssessmentExecutionType;
import com.datausher.data.quality.api.CreateQualityRuleRequest;
import com.datausher.data.quality.api.DataExecutionPolicy;
import com.datausher.data.quality.api.DataTargetRef;
import com.datausher.data.quality.api.DataTargetType;
import com.datausher.data.quality.api.ProfileMetricSpec;
import com.datausher.data.quality.api.ProfileMetricType;
import com.datausher.data.quality.api.QualityRuleId;
import com.datausher.data.quality.api.QualityRuleRef;
import com.datausher.data.quality.api.QualityRuleSpec;
import com.datausher.data.quality.api.QualityRuleType;
import com.datausher.data.quality.api.QualitySeverity;
import com.datausher.data.quality.api.StartProfileJobRequest;
import com.datausher.data.quality.api.StartQualityCheckRequest;
import com.datausher.data.quality.core.DefaultProfileService;
import com.datausher.data.quality.core.DefaultQualityCheckService;
import com.datausher.data.quality.core.DefaultQualityRuleService;
import com.datausher.data.quality.core.InMemoryProfileStore;
import com.datausher.data.quality.core.InMemoryQualityCheckStore;
import com.datausher.data.quality.core.InMemoryQualityRuleStore;
import com.datausher.data.quality.core.ProfileExecutionPlanner;
import com.datausher.data.quality.core.ProfileExecutionPlannerRegistry;
import com.datausher.data.quality.core.ProfileResultDecoderRegistry;
import com.datausher.data.quality.core.QualityCheckPlanningRequest;
import com.datausher.data.quality.core.QualityExecutionPlanner;
import com.datausher.data.quality.core.QualityExecutionPlannerRegistry;
import com.datausher.data.quality.core.QualityResultDecoderRegistry;
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
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.core.NoopDomainEventPublisher;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataUnderstandingCompositionTest {
    @Test
    void composesLineageProfilingAndQualityThroughPublicExecutionContracts() {
        var ids = new UuidIdGenerator();
        var clock = new SystemClock();
        var events = new NoopDomainEventPublisher();
        RequestContext context = RequestContext.system("request-1", Instant.now());
        var lineage = new DefaultLineageService(
                new InMemoryLineageStore(), new Sha256LineageIdFactory(),
                clock, ids, events);
        LineageNodeRef raw = new LineageNodeRef(LineageNodeType.TABLE, "raw-orders");
        LineageNodeRef curated = new LineageNodeRef(LineageNodeType.TABLE, "curated-orders");
        lineage.applySnapshot(new ApplyLineageSnapshotRequest(
                new LineageSourceRef(LineageSourceType.EXECUTION, "execution-1"),
                1, LineageSnapshotMode.REPLACE,
                List.of(
                        new LineageNodeInput(raw, "Raw orders", Map.of()),
                        new LineageNodeInput(curated, "Curated orders", Map.of())),
                List.of(new LineageEdgeInput(
                        raw, curated, LineageEdgeType.DATA_FLOW, Map.of())),
                Instant.now(), context));

        var rules = new DefaultQualityRuleService(
                new InMemoryQualityRuleStore(), clock, ids, events);
        QualityRuleId ruleId = new QualityRuleId("orders-not-null");
        rules.create(new CreateQualityRuleRequest(
                ruleId, new QualityRuleSpec(
                "Orders not null",
                new DataTargetRef(DataTargetType.COLUMN, "curated-orders.id", Map.of()),
                QualityRuleType.NOT_NULL, Map.of(), QualitySeverity.ERROR, Map.of()),
                Map.of(), context));

        RecordingExecutions executions = new RecordingExecutions();
        ProfileExecutionPlanner profilePlanner = new ProfileExecutionPlanner() {
            @Override
            public AssessmentExecutionType executionType() {
                return AssessmentExecutionType.COMPUTE;
            }

            @Override
            public ExecutionWorkload plan(com.datausher.data.quality.api.ProfileJob job) {
                return workload("profile-workload", job.target().targetId());
            }
        };
        QualityExecutionPlanner qualityPlanner = new QualityExecutionPlanner() {
            @Override
            public AssessmentExecutionType executionType() {
                return AssessmentExecutionType.COMPUTE;
            }

            @Override
            public ExecutionWorkload plan(QualityCheckPlanningRequest request) {
                return workload("quality-workload", request.check().checkId().value());
            }
        };
        var profiles = new DefaultProfileService(
                new InMemoryProfileStore(), executions, executions,
                new ProfileExecutionPlannerRegistry(List.of(profilePlanner)),
                new ProfileResultDecoderRegistry(List.of()), ids, clock, events);
        var checks = new DefaultQualityCheckService(
                new InMemoryQualityCheckStore(), rules, executions, executions,
                new QualityExecutionPlannerRegistry(List.of(qualityPlanner)),
                new QualityResultDecoderRegistry(List.of()), ids, clock, events);
        var profile = profiles.start(new StartProfileJobRequest(
                new DataTargetRef(DataTargetType.TABLE, "curated-orders", Map.of()),
                List.of(new ProfileMetricSpec(
                        ProfileMetricType.ROW_COUNT, Optional.empty(), Map.of())),
                policy(), "profile-1", Map.of(), context));
        profiles.dispatch(profile.jobId(), context);
        var check = checks.start(new StartQualityCheckRequest(
                List.of(new QualityRuleRef(ruleId, 1)), policy(),
                "check-1", Map.of(), context));
        checks.dispatch(check.checkId(), context);

        var rawNode = lineage.findNode(raw).orElseThrow();
        assertEquals(1, lineage.analyzeImpact(new ImpactAnalysisRequest(
                rawNode.nodeId(), 5, 100, Set.of(), Set.of())).candidates().size());
        assertEquals(List.of("profile-job", "quality-check"), executions.submissions.stream()
                .map(submission -> submission.origin().type().value()).toList());
    }

    private static DataExecutionPolicy policy() {
        return new DataExecutionPolicy(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                100, Map.of());
    }

    private static ExecutionWorkload workload(String type, String payload) {
        return new ExecutionWorkload(
                new ExecutionWorkloadType(type), payload, Map.of(), Map.of());
    }

    private static final class RecordingExecutions
            implements ExecutionCommandService, ExecutionQueryService {
        private final List<SubmitExecutionRequest> submissions = new ArrayList<>();
        private final List<ExecutionRequest> requests = new ArrayList<>();

        @Override
        public ExecutionRequest submit(SubmitExecutionRequest request) {
            submissions.add(request);
            Instant now = Instant.now();
            ExecutionRequest execution = new ExecutionRequest(
                    new ExecutionRequestId("execution-" + submissions.size()),
                    request.specification(), request.idempotencyKey(), request.origin(),
                    ExecutionState.QUEUED, now, now, Optional.empty(), 1);
            requests.add(execution);
            return execution;
        }

        @Override
        public ExecutionRequest cancel(CancelExecutionRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ExecutionRequest> findRequest(ExecutionRequestId requestId) {
            return requests.stream().filter(
                    request -> request.requestId().equals(requestId)).findFirst();
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
            return new PageResult<>(
                    requests, requests.size(), pageRequest.page(), pageRequest.size());
        }
    }
}
