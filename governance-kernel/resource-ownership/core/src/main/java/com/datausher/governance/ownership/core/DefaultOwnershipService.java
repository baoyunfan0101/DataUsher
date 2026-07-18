package com.datausher.governance.ownership.core;

import com.datausher.governance.access.api.IdentityQueryService;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.ownership.api.AssignResourceOwnerRequest;
import com.datausher.governance.ownership.api.OwnershipCommandService;
import com.datausher.governance.ownership.api.OwnershipQuery;
import com.datausher.governance.ownership.api.OwnershipQueryService;
import com.datausher.governance.ownership.api.RemoveResourceOwnerRequest;
import com.datausher.governance.ownership.api.ResourceOwner;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceRef;
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

public final class DefaultOwnershipService implements OwnershipCommandService, OwnershipQueryService {
    private final OwnershipStore store;
    private final ResourceQueryService resources;
    private final IdentityQueryService identities;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;

    public DefaultOwnershipService(
            OwnershipStore store,
            ResourceQueryService resources,
            IdentityQueryService identities,
            Clock clock,
            AuditedCommandExecutor commandExecutor
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
        this.identities = Objects.requireNonNull(identities, "identities must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
    }

    @Override
    public ResourceOwner assign(AssignResourceOwnerRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireActiveResource(request.resourceRef());
        requireActiveSubject(request);
        Optional<ResourceOwner> previous = store.find(request.resourceRef(), request.subjectRef(), request.role());
        ResourceOwner owner = new ResourceOwner(
                request.resourceRef(),
                request.subjectRef(),
                request.role(),
                clock.now(),
                request.requestContext().actor().actorId(),
                request.attributes()
        );
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public ResourceOwner execute() {
                store.replace(previous, Optional.of(owner));
                return owner;
            }

            @Override
            public AuditRecordRequest audit(ResourceOwner result) {
                return auditRequest(
                        request.requestContext(),
                        "resource-owner.assign",
                        result,
                        Map.of("subject", result.subjectRef().canonicalValue(), "role", result.role().value())
                );
            }

            @Override
            public void rollback(ResourceOwner result, RuntimeException cause) {
                store.replace(Optional.of(owner), previous);
            }
        });
    }

    @Override
    public void remove(RemoveResourceOwnerRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ResourceOwner current = store.find(request.resourceRef(), request.subjectRef(), request.role())
                .orElseThrow(() -> new IllegalArgumentException("resource owner does not exist"));
        commandExecutor.execute(new AuditedCommand<ResourceOwner>() {
            @Override
            public ResourceOwner execute() {
                store.replace(Optional.of(current), Optional.empty());
                return current;
            }

            @Override
            public AuditRecordRequest audit(ResourceOwner result) {
                return auditRequest(
                        request.requestContext(),
                        "resource-owner.remove",
                        result,
                        Map.of("subject", result.subjectRef().canonicalValue(), "role", result.role().value())
                );
            }

            @Override
            public void rollback(ResourceOwner result, RuntimeException cause) {
                store.replace(Optional.empty(), Optional.of(current));
            }
        });
    }

    @Override
    public List<ResourceOwner> listOwners(ResourceRef resourceRef) {
        Objects.requireNonNull(resourceRef, "resourceRef must not be null");
        List<ResourceOwner> owners = new java.util.ArrayList<>();
        int page = 1;
        long total;
        do {
            PageResult<ResourceOwner> result = store.search(
                    OwnershipQuery.forResource(resourceRef),
                    new PageRequest(page, 1000, List.of())
            );
            owners.addAll(result.items());
            total = result.total();
            page++;
        } while (owners.size() < total);
        if (owners.size() != total) {
            throw new IllegalStateException("ownership changed while all owners were being listed");
        }
        return List.copyOf(owners);
    }

    @Override
    public PageResult<ResourceOwner> search(OwnershipQuery query, PageRequest pageRequest) {
        return store.search(
                Objects.requireNonNull(query, "query must not be null"),
                Objects.requireNonNull(pageRequest, "pageRequest must not be null")
        );
    }

    private void requireActiveResource(ResourceRef ref) {
        RegisteredResource resource = resources.find(ref)
                .orElseThrow(() -> new IllegalArgumentException("resource does not exist: " + ref.canonicalValue()));
        if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
            throw new IllegalStateException("resource is not active: " + ref.canonicalValue());
        }
    }

    private void requireActiveSubject(AssignResourceOwnerRequest request) {
        Subject subject = identities.find(request.subjectRef())
                .orElseThrow(() -> new IllegalArgumentException(
                        "subject does not exist: " + request.subjectRef().canonicalValue()));
        if (subject.status() != SubjectStatus.ACTIVE) {
            throw new IllegalStateException(
                    "subject is not active: " + request.subjectRef().canonicalValue());
        }
    }

    private static AuditRecordRequest auditRequest(
            com.datausher.platform.shared.context.RequestContext context,
            String action,
            ResourceOwner owner,
            Map<String, String> details
    ) {
        ResourceRef ref = owner.resourceRef();
        return new AuditRecordRequest(
                context,
                "resource-ownership",
                action,
                new AuditTarget(ref.resourceType(), ref.resourceId(), ref.scope().canonicalValue()),
                AuditOutcome.SUCCEEDED,
                details
        );
    }
}
