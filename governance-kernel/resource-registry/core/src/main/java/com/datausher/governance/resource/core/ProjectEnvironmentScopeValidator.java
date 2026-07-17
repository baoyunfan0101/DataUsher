package com.datausher.governance.resource.core;

import com.datausher.governance.project.api.Environment;
import com.datausher.governance.project.api.EnvironmentQueryService;
import com.datausher.governance.project.api.EnvironmentStatus;
import com.datausher.governance.project.api.Project;
import com.datausher.governance.project.api.ProjectQueryService;
import com.datausher.governance.project.api.ProjectStatus;
import com.datausher.governance.resource.api.ResourceScope;
import com.datausher.governance.resource.api.ResourceScopeType;

import java.util.Objects;

public final class ProjectEnvironmentScopeValidator implements ResourceScopeValidator {
    private final ProjectQueryService projectQueryService;
    private final EnvironmentQueryService environmentQueryService;

    public ProjectEnvironmentScopeValidator(
            ProjectQueryService projectQueryService,
            EnvironmentQueryService environmentQueryService
    ) {
        this.projectQueryService = Objects.requireNonNull(projectQueryService,
                "projectQueryService must not be null");
        this.environmentQueryService = Objects.requireNonNull(environmentQueryService,
                "environmentQueryService must not be null");
    }

    @Override
    public void validate(ResourceScope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        if (scope.type() == ResourceScopeType.GLOBAL) {
            return;
        }
        Project project = projectQueryService.findProjectById(scope.projectId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "resource scope references an unknown project: " + scope.projectId()));
        if (project.status() != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("resource scope references an inactive project: " + scope.projectId());
        }
        if (scope.type() == ResourceScopeType.ENVIRONMENT) {
            Environment environment = environmentQueryService.findEnvironmentById(scope.environmentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "resource scope references an unknown environment: " + scope.environmentId()));
            if (!environment.projectId().equals(project.projectId())) {
                throw new IllegalArgumentException("environment does not belong to the scoped project");
            }
            if (environment.status() != EnvironmentStatus.ACTIVE) {
                throw new IllegalStateException("resource scope references an inactive environment: "
                        + scope.environmentId());
            }
        }
    }
}
