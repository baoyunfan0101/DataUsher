package com.datausher.data.quality.core;

import com.datausher.data.quality.api.DataExecutionPolicy;
import com.datausher.data.quality.api.DataTargetRef;
import com.datausher.data.quality.api.DataTargetType;
import com.datausher.data.quality.api.ProfileJobState;
import com.datausher.data.quality.api.ProfileMetric;
import com.datausher.data.quality.api.ProfileMetricSpec;
import com.datausher.data.quality.api.ProfileMetricType;
import com.datausher.data.quality.api.StartProfileJobRequest;
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
import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.execution.api.SubmitExecutionRequest;
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

class DefaultProfileServiceTest {
    private static final ExecutionWorkloadType WORKLOAD_TYPE =
            new ExecutionWorkloadType("profile-fixture");

    @Test
    void submitsProfilesIdempotentlyAndCollectsTypedMetrics() {
        RecordingExecutions executions = new RecordingExecutions();
        ProfileExecutionPlanner planner = new ProfileExecutionPlanner() {
            @Override
            public com.datausher.data.quality.api.AssessmentExecutionType executionType() {
                return com.datausher.data.quality.api.AssessmentExecutionType.COMPUTE;
            }

            @Override
            public ExecutionWorkload plan(com.datausher.data.quality.api.ProfileJob job) {
                return new ExecutionWorkload(
                        WORKLOAD_TYPE, job.target().targetId(), Map.of(), Map.of());
            }
        };
        ProfileResultDecoder decoder = new ProfileResultDecoder() {
            @Override
            public ExecutionWorkloadType workloadType() {
                return WORKLOAD_TYPE;
            }

            @Override
            public List<ProfileMetric> decode(ProfileResultDecodingRequest request) {
                return List.of(new ProfileMetric(
                        request.job().jobId(), ProfileMetricType.ROW_COUNT,
                        Optional.empty(), new ExecutionValue.DecimalValue(
                        java.math.BigDecimal.valueOf(42)), Map.of(), Instant.now()));
            }
        };
        var service = new DefaultProfileService(
                new InMemoryProfileStore(), executions, executions,
                new ProfileExecutionPlannerRegistry(List.of(planner)),
                new ProfileResultDecoderRegistry(List.of(decoder)),
                new UuidIdGenerator(), new SystemClock());
        RequestContext context = RequestContext.system("request-1", Instant.now());
        StartProfileJobRequest request = new StartProfileJobRequest(
                new DataTargetRef(DataTargetType.TABLE, "orders", Map.of()),
                List.of(new ProfileMetricSpec(
                        ProfileMetricType.ROW_COUNT, Optional.empty(), Map.of())),
                new DataExecutionPolicy(
                        new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                        100, Map.of()), "profile-key", Map.of(), context);

        var first = service.start(request);
        var duplicate = service.start(request);
        var submitted = service.dispatch(first.jobId(), context);
        service.handleExecutionStateChanged(completed(
                executions.requests.get(submitted.executionRequestId().orElseThrow()), context));

        assertEquals(first.jobId(), duplicate.jobId());
        assertEquals(ProfileJobState.SUCCEEDED,
                service.findJob(first.jobId()).orElseThrow().state());
        assertEquals(java.math.BigDecimal.valueOf(42),
                ((ExecutionValue.DecimalValue) service.listMetrics(first.jobId())
                        .getFirst().value()).value());
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
                "event-1", now, context, completed, Optional.empty());
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
