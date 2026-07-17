package com.datausher.governance.resource.core;

import com.datausher.governance.resource.api.RegisteredResource;
import com.datausher.governance.resource.api.RegisterResourceTypeRequest;
import com.datausher.governance.resource.api.ResourceLifecycle;
import com.datausher.governance.resource.api.ResourceQuery;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.governance.resource.api.ResourceScope;
import com.datausher.governance.resource.api.ResourceTypeDefinition;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.time.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryResourceStoreTest {
    @Test
    void keepsTypeOwnershipStableAndSearchesByScope() {
        InMemoryResourceStore store = new InMemoryResourceStore();
        ResourceTypeDefinition type = new ResourceTypeDefinition(
                "table", "metadata-catalog", "Table", Set.of("read"));
        store.registerType(type);
        store.registerType(type);

        ResourceRef ref = new ResourceRef(
                "table", "table-1", ResourceScope.project("project-1"));
        store.create(new RegisteredResource(
                ref,
                "Orders",
                ResourceLifecycle.ACTIVE,
                Instant.parse("2026-07-17T00:00:00Z"),
                "system",
                Map.of()
        ));

        assertEquals(1, store.search(new ResourceQuery(
                "table", ResourceScope.project("project-1"), ResourceLifecycle.ACTIVE),
                PageRequest.firstPage()).total());
        assertThrows(IllegalStateException.class, () -> store.registerType(
                new ResourceTypeDefinition("table", "other-module", "Table", Set.of("read"))));
    }

    @Test
    void appliesResourcePaginationInsideTheStore() {
        InMemoryResourceStore store = new InMemoryResourceStore();
        store.create(resource("table-2"));
        store.create(resource("table-1"));

        var result = store.search(
                new ResourceQuery(null, null, null),
                new PageRequest(2, 1, List.of())
        );

        assertEquals(2, result.total());
        assertEquals("table-2", result.items().get(0).ref().resourceId());
    }

    private static RegisteredResource resource(String resourceId) {
        return new RegisteredResource(
                ResourceRef.global("table", resourceId),
                resourceId,
                ResourceLifecycle.ACTIVE,
                Instant.parse("2026-07-17T00:00:00Z"),
                "system",
                Map.of()
        );
    }

    @Test
    void removesNewResourceTypeWhenAuditExecutionFails() {
        InMemoryResourceStore store = new InMemoryResourceStore();
        DefaultResourceRegistryService service = new DefaultResourceRegistryService(
                store,
                fixedClock(),
                failingExecutor(),
                scope -> {
                }
        );

        assertThrows(IllegalStateException.class, () -> service.register(new RegisterResourceTypeRequest(
                new ResourceTypeDefinition("table", "metadata-catalog", "Table", Set.of("read")),
                RequestContext.system("request-1", Instant.parse("2026-07-17T00:00:00Z"))
        )));

        assertTrue(store.findType("table").isEmpty());
    }

    private static AuditedCommandExecutor failingExecutor() {
        return new AuditedCommandExecutor() {
            @Override
            public <T> T execute(AuditedCommand<T> command) {
                T result = command.execute();
                RuntimeException failure = new IllegalStateException("audit unavailable");
                command.rollback(result, failure);
                throw failure;
            }
        };
    }

    private static Clock fixedClock() {
        return new Clock() {
            @Override
            public Instant now() {
                return Instant.parse("2026-07-17T00:00:00Z");
            }

            @Override
            public ZoneId zone() {
                return ZoneOffset.UTC;
            }
        };
    }
}
