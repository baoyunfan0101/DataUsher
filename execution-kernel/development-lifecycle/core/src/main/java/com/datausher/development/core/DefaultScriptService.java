package com.datausher.development.core;

import com.datausher.development.api.CreateScriptRequest;
import com.datausher.development.api.CreateScriptVersionRequest;
import com.datausher.development.api.ScriptCommandService;
import com.datausher.development.api.ScriptDefinition;
import com.datausher.development.api.ScriptCreatedEvent;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptQueryService;
import com.datausher.development.api.ScriptVersion;
import com.datausher.development.api.ScriptVersionCreatedEvent;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultScriptService implements ScriptCommandService, ScriptQueryService {
    private final ScriptStore store;
    private final ResourceQueryService resources;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;

    public DefaultScriptService(
            ScriptStore store,
            ResourceQueryService resources,
            Clock clock,
            AuditedCommandExecutor commandExecutor,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public ScriptDefinition create(CreateScriptRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireActiveResource(request.resourceRef());
        Instant now = clock.now();
        ScriptDefinition script = new ScriptDefinition(
                request.scriptId(), request.resourceRef(), request.displayName(), request.language(),
                0, 1, now, now, request.requestContext().actor().actorId(), request.attributes());
        ScriptDefinition created = commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ScriptDefinition execute() {
                store.createScript(script);
                return script;
            }

            @Override
            public AuditRecordRequest audit(ScriptDefinition result) {
                return auditRequest(request.requestContext(), "script.create", result,
                        Map.of("resource", result.resourceRef().canonicalValue()));
            }

            @Override
            public void rollback(ScriptDefinition result, RuntimeException cause) {
                store.deleteScript(result);
            }
        });
        eventPublisher.publish(new ScriptCreatedEvent(
                nextEventId(), now, request.requestContext(), created));
        return created;
    }

    @Override
    public ScriptVersion createVersion(CreateScriptVersionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ScriptDefinition current = store.findScript(request.scriptId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "script does not exist: " + request.scriptId()));
        if (current.revision() != request.expectedRevision()) {
            throw new IllegalStateException("script revision does not match expectedRevision");
        }
        long versionNumber = current.latestVersion() + 1;
        Instant now = clock.now();
        ScriptVersion version = new ScriptVersion(
                current.scriptId(), versionNumber, request.executionSpecification(), now,
                request.requestContext().actor().actorId(), request.attributes());
        ScriptDefinition updated = new ScriptDefinition(
                current.scriptId(), current.resourceRef(), current.displayName(), current.language(),
                versionNumber, current.revision() + 1, current.createdAt(), now,
                current.createdBy(), current.attributes());
        ScriptVersion created = commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ScriptVersion execute() {
                store.createVersion(current, updated, version);
                return version;
            }

            @Override
            public AuditRecordRequest audit(ScriptVersion result) {
                return auditRequest(request.requestContext(), "script.version.create", updated,
                        Map.of("version", Long.toString(result.version())));
            }

            @Override
            public void rollback(ScriptVersion result, RuntimeException cause) {
                store.deleteVersion(updated, current, result);
            }
        });
        eventPublisher.publish(new ScriptVersionCreatedEvent(
                nextEventId(), now, request.requestContext(), created));
        return created;
    }

    @Override
    public Optional<ScriptDefinition> findScript(ScriptId scriptId) {
        return store.findScript(Objects.requireNonNull(scriptId, "scriptId must not be null"));
    }

    @Override
    public Optional<ScriptVersion> findVersion(ScriptId scriptId, long version) {
        return store.findVersion(Objects.requireNonNull(scriptId, "scriptId must not be null"), version);
    }

    @Override
    public Optional<ScriptVersion> findLatestVersion(ScriptId scriptId) {
        ScriptDefinition script = store.findScript(
                Objects.requireNonNull(scriptId, "scriptId must not be null")).orElse(null);
        return script == null || script.latestVersion() == 0
                ? Optional.empty() : store.findVersion(scriptId, script.latestVersion());
    }

    @Override
    public List<ScriptVersion> listVersions(ScriptId scriptId) {
        return store.listVersions(Objects.requireNonNull(scriptId, "scriptId must not be null"));
    }

    private void requireActiveResource(com.datausher.governance.resource.api.ResourceRef resourceRef) {
        RegisteredResource resource = resources.find(resourceRef)
                .orElseThrow(() -> new IllegalArgumentException("resource does not exist: " + resourceRef));
        if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
            throw new IllegalStateException("resource is not active: " + resourceRef);
        }
    }

    private static AuditRecordRequest auditRequest(
            RequestContext context,
            String action,
            ScriptDefinition script,
            Map<String, String> details
    ) {
        return new AuditRecordRequest(
                context, "development-lifecycle", action,
                AuditTarget.global("script", script.scriptId().value()),
                AuditOutcome.SUCCEEDED, details);
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(IdGenerationRequest.of("development", "domain-event"));
    }
}
