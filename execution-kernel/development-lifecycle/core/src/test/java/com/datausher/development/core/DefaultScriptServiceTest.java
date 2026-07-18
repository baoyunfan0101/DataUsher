package com.datausher.development.core;

import com.datausher.development.api.CreateScriptRequest;
import com.datausher.development.api.CreateScriptVersionRequest;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptLanguage;
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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultScriptServiceTest {
    @Test
    void createsScriptAndImmutableVersionsWithRevisionChecks() {
        ResourceRef resourceRef = ResourceRef.global("script", "daily-orders");
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var events = new java.util.ArrayList<com.datausher.platform.shared.event.DomainEvent>();
        var service = new DefaultScriptService(
                new InMemoryScriptStore(), resources(resourceRef), clock,
                new CompensatingAuditedCommandExecutor(audit), ids, events::add);
        RequestContext context = RequestContext.system("request-1", Instant.now());
        ScriptId scriptId = new ScriptId("daily-orders");
        var script = service.create(new CreateScriptRequest(
                scriptId, resourceRef, "Daily Orders", new ScriptLanguage("custom.lang"),
                Map.of(), context));

        var version = service.createVersion(new CreateScriptVersionRequest(
                scriptId, script.revision(), specification(), Map.of("owner", "analytics"), context));

        assertEquals(1, version.version());
        assertEquals(version, service.findLatestVersion(scriptId).orElseThrow());
        assertEquals(2, service.findScript(scriptId).orElseThrow().revision());
        assertThrows(IllegalStateException.class, () -> service.createVersion(
                new CreateScriptVersionRequest(scriptId, 1, specification(), Map.of(), context)));
        assertEquals(java.util.List.of(
                        "development.script-created", "development.script-version-created"),
                events.stream().map(com.datausher.platform.shared.event.DomainEvent::eventType).toList());
    }

    private static ExecutionSpecification specification() {
        return new ExecutionSpecification(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                new ExecutionWorkload(
                        new ExecutionWorkloadType("vendor-task"), "payload", Map.of(), Map.of()),
                ExecutionResultMode.REFERENCE, 100);
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
