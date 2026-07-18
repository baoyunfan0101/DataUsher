package com.datausher.governance.ownership.core;

import com.datausher.governance.access.api.IdentityQueryService;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.ownership.api.AssignResourceOwnerRequest;
import com.datausher.governance.ownership.api.OwnershipRole;
import com.datausher.governance.ownership.api.RemoveResourceOwnerRequest;
import com.datausher.governance.ownership.api.ResourceOwnerAssignedEvent;
import com.datausher.governance.ownership.api.ResourceOwnerRemovedEvent;
import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceQueryService;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceScope;
import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.event.DomainEvent;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultOwnershipServiceTest {
    @Test
    void assignsQueriesAndRemovesOwners() {
        ResourceRef resourceRef = ResourceRef.global("table", "orders");
        SubjectRef subjectRef = new SubjectRef(SubjectType.USER, "owner-1");
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        List<DomainEvent> events = new ArrayList<>();
        var service = new DefaultOwnershipService(
                new InMemoryOwnershipStore(),
                resources(resourceRef),
                identities(subjectRef),
                ids,
                clock,
                new CompensatingAuditedCommandExecutor(audit),
                events::add
        );
        RequestContext context = RequestContext.system("request-1", Instant.now());

        service.assign(new AssignResourceOwnerRequest(
                resourceRef, subjectRef, new OwnershipRole("data-steward"), Map.of(), context));

        assertEquals(1, service.listOwners(resourceRef).size());

        service.remove(new RemoveResourceOwnerRequest(
                resourceRef, subjectRef, new OwnershipRole("data-steward"), context));

        assertTrue(service.listOwners(resourceRef).isEmpty());
        assertEquals(ResourceOwnerAssignedEvent.class, events.get(0).getClass());
        assertEquals(ResourceOwnerRemovedEvent.class, events.get(1).getClass());
    }

    @Test
    void listsEveryOwnerAcrossStorePages() {
        ResourceRef resourceRef = ResourceRef.global("table", "orders");
        InMemoryOwnershipStore store = new InMemoryOwnershipStore();
        Instant assignedAt = Instant.parse("2026-07-18T00:00:00Z");
        IntStream.range(0, 1001).forEach(index -> {
            var owner = new com.datausher.governance.ownership.api.ResourceOwner(
                    resourceRef,
                    new SubjectRef(SubjectType.USER, "owner-" + index),
                    OwnershipRole.TECHNICAL,
                    assignedAt,
                    "system",
                    Map.of()
            );
            store.replace(Optional.empty(), Optional.of(owner));
        });
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        var service = new DefaultOwnershipService(
                store,
                resources(resourceRef),
                identities(new SubjectRef(SubjectType.USER, "unused")),
                ids,
                clock,
                new CompensatingAuditedCommandExecutor(audit),
                event -> {
                }
        );

        assertEquals(1001, service.listOwners(resourceRef).size());
    }

    private static ResourceQueryService resources(ResourceRef ref) {
        RegisteredResource resource = new RegisteredResource(
                ref, "Orders", ResourceLifecycle.ACTIVE, Instant.now(), "system", Map.of());
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

    private static IdentityQueryService identities(SubjectRef ref) {
        Subject subject = new Subject(ref, "Owner", SubjectStatus.ACTIVE, Map.of());
        return new IdentityQueryService() {
            @Override
            public Optional<Subject> find(SubjectRef candidate) {
                return ref.equals(candidate) ? Optional.of(subject) : Optional.empty();
            }

            @Override
            public PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest) {
                return PageResult.empty(pageRequest);
            }
        };
    }
}
