package com.datausher.governance.resource.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceRefTest {
    @Test
    void createsStableCanonicalReferencesAtEveryScope() {
        assertEquals("datasource:global:source-1",
                ResourceRef.global("datasource", "source-1").canonicalValue());
        assertEquals("table:project/project-1:table-1",
                new ResourceRef("table", "table-1", ResourceScope.project("project-1"))
                        .canonicalValue());
        assertEquals("workflow:environment/project-1/env-1:flow-1",
                new ResourceRef(
                        "workflow",
                        "flow-1",
                        ResourceScope.environment("project-1", "env-1")
                ).canonicalValue());
    }

    @Test
    void rejectsIncompleteEnvironmentScope() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResourceScope(ResourceScopeType.ENVIRONMENT, "project-1", null));
    }
}
