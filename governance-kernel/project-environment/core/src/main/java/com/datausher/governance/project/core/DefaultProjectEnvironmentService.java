package com.datausher.governance.project.core;

import com.datausher.governance.project.api.ChangeProjectStatusRequest;
import com.datausher.governance.project.api.CreateProjectRequest;
import com.datausher.governance.project.api.Environment;
import com.datausher.governance.project.api.EnvironmentQueryService;
import com.datausher.governance.project.api.EnvironmentSpec;
import com.datausher.governance.project.api.EnvironmentStatus;
import com.datausher.governance.project.api.Project;
import com.datausher.governance.project.api.ProjectCommandService;
import com.datausher.governance.project.api.ProjectQueryService;
import com.datausher.governance.project.api.ProjectStatus;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultProjectEnvironmentService
        implements ProjectCommandService, ProjectQueryService, EnvironmentQueryService {
    private static final IdGenerationRequest PROJECT_ID_REQUEST =
            IdGenerationRequest.of("governance-kernel", "project");
    private static final IdGenerationRequest ENVIRONMENT_ID_REQUEST =
            IdGenerationRequest.of("governance-kernel", "environment");

    private final ProjectEnvironmentStore store;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;

    public DefaultProjectEnvironmentService(
            ProjectEnvironmentStore store,
            IdGenerator idGenerator,
            Clock clock,
            AuditedCommandExecutor commandExecutor
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
    }

    @Override
    public Project create(CreateProjectRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        Project project = new Project(
                idGenerator.nextIdValue(PROJECT_ID_REQUEST),
                request.key(),
                request.displayName(),
                ProjectStatus.ACTIVE,
                now,
                request.requestContext().actor().actorId(),
                request.attributes()
        );
        List<Environment> environments = new ArrayList<>();
        for (EnvironmentSpec spec : request.environments()) {
            environments.add(new Environment(
                    idGenerator.nextIdValue(ENVIRONMENT_ID_REQUEST),
                    project.projectId(),
                    spec.key(),
                    spec.displayName(),
                    EnvironmentStatus.ACTIVE,
                    now
            ));
        }
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public Project execute() {
                store.create(project, environments);
                return project;
            }

            @Override
            public AuditRecordRequest audit(Project result) {
                return new AuditRecordRequest(
                        request.requestContext(),
                        "project-environment",
                        "project.create",
                        AuditTarget.global("project", result.projectId()),
                        AuditOutcome.SUCCEEDED,
                        Map.of("projectKey", result.key())
                );
            }

            @Override
            public void rollback(Project result, RuntimeException cause) {
                store.delete(result, environments);
            }
        });
    }

    @Override
    public Project changeStatus(ChangeProjectStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Project current = store.findProjectById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("project does not exist: " + request.projectId()));
        validateStatusTransition(current.status(), request.status());
        Project updated = current.withStatus(request.status());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public Project execute() {
                store.update(current, updated);
                return updated;
            }

            @Override
            public AuditRecordRequest audit(Project result) {
                return new AuditRecordRequest(
                        request.requestContext(),
                        "project-environment",
                        "project.status.change",
                        AuditTarget.global("project", result.projectId()),
                        AuditOutcome.SUCCEEDED,
                        Map.of("from", current.status().name(), "to", result.status().name())
                );
            }

            @Override
            public void rollback(Project result, RuntimeException cause) {
                store.update(updated, current);
            }
        });
    }

    @Override
    public Optional<Project> findProjectById(String projectId) {
        return store.findProjectById(normalize(projectId, "projectId"));
    }

    @Override
    public Optional<Project> findByKey(String key) {
        return store.findProjectByKey(normalize(key, "key").toLowerCase());
    }

    @Override
    public PageResult<Project> list(PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<Project> projects = store.listProjects();
        int fromIndex = (int) Math.min(pageRequest.offset(), projects.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), projects.size());
        return new PageResult<>(projects.subList(fromIndex, toIndex), projects.size(),
                pageRequest.page(), pageRequest.size());
    }

    @Override
    public Optional<Environment> findEnvironmentById(String environmentId) {
        return store.findEnvironmentById(normalize(environmentId, "environmentId"));
    }

    @Override
    public Optional<Environment> findByProjectAndKey(String projectId, String key) {
        return store.findEnvironment(normalize(projectId, "projectId"), normalize(key, "key").toLowerCase());
    }

    @Override
    public List<Environment> listByProject(String projectId) {
        return store.listEnvironments(normalize(projectId, "projectId"));
    }

    private static void validateStatusTransition(ProjectStatus current, ProjectStatus next) {
        if (current == ProjectStatus.ARCHIVED && next != ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("an archived project cannot be reactivated");
        }
    }

    private static String normalize(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
