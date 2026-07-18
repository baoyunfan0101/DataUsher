package com.datausher.governance.ownership.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.governance.resource.api.ResourceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnershipContractTest {
    @Test
    void ownerValuesAreImmutableAndRolesAreExtensible() {
        Map<String, String> attributes = new HashMap<>(Map.of("source", "catalog"));
        ResourceOwner owner = new ResourceOwner(
                ResourceRef.global("table", "orders"),
                new SubjectRef(SubjectType.USER, "owner-1"),
                new OwnershipRole("data-steward"),
                Instant.parse("2026-07-18T00:00:00Z"),
                "admin",
                attributes
        );
        attributes.put("source", "changed");

        assertEquals("data-steward", owner.role().value());
        assertEquals(Map.of("source", "catalog"), owner.attributes());
    }
}
