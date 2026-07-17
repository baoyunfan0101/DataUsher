package com.datausher.governance.resource.core;

import com.datausher.governance.resource.api.ChangeResourceLifecycleRequest;
import com.datausher.governance.resource.api.RegisterResourceRequest;
import com.datausher.governance.resource.api.RegisterResourceTypeRequest;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceCommandService;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.governance.resource.api.ResourceTypeRegistry;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultResourceRegistryService
        implements ResourceTypeRegistry, ResourceCommandService, ResourceQueryService {
    private final ResourceStore store;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;
    private final ResourceScopeValidator scopeValidator;

    public DefaultResourceRegistryService(
            ResourceStore store,
            Clock clock,
            AuditedCommandExecutor commandExecutor,
            ResourceScopeValidator scopeValidator
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator must not be null");
    }

    @Override
    public ResourceTypeDefinition register(RegisterResourceTypeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ResourceTypeDefinition definition = request.definition();
        ResourceTypeRegistration registration = commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ResourceTypeRegistration execute() {
                return store.registerType(definition);
            }

            @Override
            public AuditRecordRequest audit(ResourceTypeRegistration result) {
                return new AuditRecordRequest(
                        request.requestContext(),
                        "resource-registry",
                        "resource-type.register",
                        AuditTarget.global("resource-type", result.definition().resourceType()),
                        AuditOutcome.SUCCEEDED,
                        Map.of(
                                "ownerModule", result.definition().ownerModule(),
                                "supportedActions", supportedActions(result.definition())
                        )
                );
            }

            @Override
            public void rollback(ResourceTypeRegistration result, RuntimeException cause) {
                if (result.created()) {
                    store.unregisterType(result.definition());
                }
            }
        });
        return registration.definition();
    }

    @Override
    public Optional<ResourceTypeDefinition> find(String resourceType) {
        String normalized = new ResourceRef(resourceType, "validation",
                com.datausher.governance.resource.api.ResourceScope.global()).resourceType();
        return store.findType(normalized);
    }

    @Override
    public List<ResourceTypeDefinition> list() {
        return store.listTypes();
    }

    @Override
    public RegisteredResource register(RegisterResourceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ResourceTypeDefinition type = store.findType(request.ref().resourceType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "resource type is not registered: " + request.ref().resourceType()));
        scopeValidator.validate(request.ref().scope());
        RegisteredResource resource = new RegisteredResource(
                request.ref(),
                request.displayName(),
                ResourceLifecycle.ACTIVE,
                clock.now(),
                request.requestContext().actor().actorId(),
                request.attributes()
        );
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public RegisteredResource execute() {
                store.create(resource);
                return resource;
            }

            @Override
            public AuditRecordRequest audit(RegisteredResource result) {
                return new AuditRecordRequest(
                        request.requestContext(),
                        "resource-registry",
                        "resource.register",
                        toAuditTarget(result.ref()),
                        AuditOutcome.SUCCEEDED,
                        Map.of(
                                "ownerModule", type.ownerModule(),
                                "supportedActions", supportedActions(type)
                        )
                );
            }

            @Override
            public void rollback(RegisteredResource result, RuntimeException cause) {
                store.delete(result);
            }
        });
    }

    @Override
    public RegisteredResource changeLifecycle(ChangeResourceLifecycleRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        RegisteredResource current = store.find(request.ref())
                .orElseThrow(() -> new IllegalArgumentException(
                        "resource does not exist: " + request.ref().canonicalValue()));
        if (current.lifecycle() == ResourceLifecycle.DELETED && request.lifecycle() != ResourceLifecycle.DELETED) {
            throw new IllegalStateException("a deleted resource cannot be reactivated");
        }
        RegisteredResource updated = current.withLifecycle(request.lifecycle());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public RegisteredResource execute() {
                store.update(current, updated);
                return updated;
            }

            @Override
            public AuditRecordRequest audit(RegisteredResource result) {
                return new AuditRecordRequest(
                        request.requestContext(),
                        "resource-registry",
                        "resource.lifecycle.change",
                        toAuditTarget(result.ref()),
                        AuditOutcome.SUCCEEDED,
                        Map.of("from", current.lifecycle().name(), "to", result.lifecycle().name())
                );
            }

            @Override
            public void rollback(RegisteredResource result, RuntimeException cause) {
                store.update(updated, current);
            }
        });
    }

    @Override
    public Optional<RegisteredResource> find(ResourceRef ref) {
        return store.find(Objects.requireNonNull(ref, "ref must not be null"));
    }

    @Override
    public PageResult<RegisteredResource> search(ResourceQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<RegisteredResource> resources = store.search(query);
        int fromIndex = (int) Math.min(pageRequest.offset(), resources.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), resources.size());
        return new PageResult<>(resources.subList(fromIndex, toIndex), resources.size(),
                pageRequest.page(), pageRequest.size());
    }

    private static AuditTarget toAuditTarget(ResourceRef ref) {
        return new AuditTarget(ref.resourceType(), ref.resourceId(), ref.scope().canonicalValue());
    }

    private static String supportedActions(ResourceTypeDefinition type) {
        return type.actions().stream().sorted().collect(Collectors.joining(","));
    }
}
