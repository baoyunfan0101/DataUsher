package com.datausher.workflow.core;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.execution.api.ExecutionResultMode;
import com.datausher.execution.api.ExecutionSpecification;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.core.SystemClock;
import com.datausher.workflow.api.CreateWorkflowRequest;
import com.datausher.workflow.api.CreateWorkflowVersionRequest;
import com.datausher.workflow.api.TaskRetryPolicy;
import com.datausher.workflow.api.WorkflowId;
import com.datausher.workflow.api.WorkflowTaskDefinition;
import com.datausher.workflow.api.WorkflowVersionSpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultWorkflowServiceTest {
    @Test
    void createsWorkflowAndImmutableVersionsWithRevisionChecks() {
        ResourceRef resourceRef = ResourceRef.global("workflow", "daily-orders");
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var service = new DefaultWorkflowService(
                new InMemoryWorkflowStore(), resources(resourceRef), clock,
                new CompensatingAuditedCommandExecutor(audit));
        RequestContext context = RequestContext.system("request-1", Instant.now());
        WorkflowId workflowId = new WorkflowId("daily-orders");
        var workflow = service.create(new CreateWorkflowRequest(
                workflowId, resourceRef, "Daily Orders", Map.of(), context));

        var version = service.createVersion(new CreateWorkflowVersionRequest(
                workflowId, workflow.revision(), new WorkflowVersionSpec(
                        List.of(task("extract")), List.of(), Optional.empty(), Map.of()), context));

        assertEquals(1, version.version());
        assertEquals(version, service.findLatestVersion(workflowId).orElseThrow());
        assertEquals(2, service.findWorkflow(workflowId).orElseThrow().revision());
    }

    private static WorkflowTaskDefinition task(String key) {
        return new WorkflowTaskDefinition(
                key, "Extract",
                new ExecutionSpecification(
                        new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                        new ExecutionWorkload(new ExecutionWorkloadType("fixture"), key, Map.of(), Map.of()),
                        ExecutionResultMode.DISCARD, 100),
                TaskRetryPolicy.NONE, Duration.ofMinutes(10), Map.of());
    }

    private static ResourceQueryService resources(ResourceRef ref) {
        RegisteredResource resource = new RegisteredResource(
                ref, "Daily Orders", ResourceLifecycle.ACTIVE, Instant.now(), "system", Map.of());
        return new ResourceQueryService() {
            @Override
            public Optional<RegisteredResource> find(ResourceRef candidate) {
                return ref.equals(candidate) ? Optional.of(resource) : Optional.empty();
            }

            @Override
            public PageResult<RegisteredResource> search(ResourceQuery query, PageRequest pageRequest) {
                return PageResult.empty(pageRequest);
            }
        };
    }
}
