package com.datausher.governance.project.core;

import com.datausher.governance.project.api.Environment;
import com.datausher.governance.project.api.EnvironmentStatus;
import com.datausher.governance.project.api.Project;
import com.datausher.governance.project.api.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryProjectEnvironmentStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void createsProjectAndEnvironmentsAsOneUniqueUnit() {
        InMemoryProjectEnvironmentStore store = new InMemoryProjectEnvironmentStore();
        Project project = project("project-1", "analytics");
        List<Environment> environments = List.of(
                environment("env-prod", project.projectId(), "prod"),
                environment("env-dev", project.projectId(), "dev")
        );

        store.create(project, environments);

        assertEquals(project, store.findProjectByKey("analytics").orElseThrow());
        assertEquals(List.of("dev", "prod"), store.listEnvironments(project.projectId()).stream()
                .map(Environment::key)
                .toList());
        assertThrows(IllegalStateException.class, () -> store.create(
                project("project-2", "analytics"), List.of()));
    }

    private static Project project(String projectId, String key) {
        return new Project(projectId, key, "Analytics", ProjectStatus.ACTIVE, NOW, "system", Map.of());
    }

    private static Environment environment(String environmentId, String projectId, String key) {
        return new Environment(
                environmentId, projectId, key, key, EnvironmentStatus.ACTIVE, NOW);
    }
}
