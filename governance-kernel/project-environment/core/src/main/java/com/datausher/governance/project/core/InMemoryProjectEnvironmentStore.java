package com.datausher.governance.project.core;

import com.datausher.governance.project.api.Environment;
import com.datausher.governance.project.api.Project;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProjectEnvironmentStore implements ProjectEnvironmentStore {
    private final Map<String, Project> projectsById = new ConcurrentHashMap<>();
    private final Map<String, String> projectIdsByKey = new ConcurrentHashMap<>();
    private final Map<String, Environment> environmentsById = new ConcurrentHashMap<>();
    private final Map<String, String> environmentIdsByProjectAndKey = new ConcurrentHashMap<>();

    @Override
    public synchronized void create(Project project, List<Environment> environments) {
        if (projectsById.containsKey(project.projectId())) {
            throw new IllegalStateException("project ID already exists: " + project.projectId());
        }
        if (projectIdsByKey.containsKey(project.key())) {
            throw new IllegalStateException("project key already exists: " + project.key());
        }
        for (Environment environment : environments) {
            String lookupKey = environmentLookupKey(environment.projectId(), environment.key());
            if (environmentsById.containsKey(environment.environmentId())
                    || environmentIdsByProjectAndKey.containsKey(lookupKey)) {
                throw new IllegalStateException("environment already exists: " + environment.key());
            }
        }
        projectsById.put(project.projectId(), project);
        projectIdsByKey.put(project.key(), project.projectId());
        for (Environment environment : environments) {
            environmentsById.put(environment.environmentId(), environment);
            environmentIdsByProjectAndKey.put(
                    environmentLookupKey(environment.projectId(), environment.key()),
                    environment.environmentId()
            );
        }
    }

    @Override
    public synchronized void delete(Project project, List<Environment> environments) {
        if (!project.equals(projectsById.get(project.projectId()))) {
            throw new IllegalStateException("project changed before rollback: " + project.projectId());
        }
        for (Environment environment : environments) {
            if (!environment.equals(environmentsById.get(environment.environmentId()))) {
                throw new IllegalStateException(
                        "environment changed before rollback: " + environment.environmentId());
            }
        }
        for (Environment environment : environments) {
            environmentsById.remove(environment.environmentId(), environment);
            environmentIdsByProjectAndKey.remove(
                    environmentLookupKey(environment.projectId(), environment.key()),
                    environment.environmentId()
            );
        }
        projectsById.remove(project.projectId(), project);
        projectIdsByKey.remove(project.key(), project.projectId());
    }

    @Override
    public void update(Project expected, Project replacement) {
        if (!expected.projectId().equals(replacement.projectId())) {
            throw new IllegalArgumentException("project IDs must match");
        }
        if (!projectsById.replace(expected.projectId(), expected, replacement)) {
            throw new IllegalStateException("project changed concurrently: " + expected.projectId());
        }
    }

    @Override
    public Optional<Project> findProjectById(String projectId) {
        return Optional.ofNullable(projectsById.get(projectId));
    }

    @Override
    public Optional<Project> findProjectByKey(String key) {
        return Optional.ofNullable(projectIdsByKey.get(key)).map(projectsById::get);
    }

    @Override
    public PageResult<Project> listProjects(PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<Project> projects = new ArrayList<>(projectsById.values());
        projects.sort(Comparator.comparing(Project::key));
        int fromIndex = (int) Math.min(pageRequest.offset(), projects.size());
        int toIndex = (int) Math.min(
                (long) fromIndex + pageRequest.size(), projects.size());
        return new PageResult<>(
                projects.subList(fromIndex, toIndex),
                projects.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }

    @Override
    public Optional<Environment> findEnvironmentById(String environmentId) {
        return Optional.ofNullable(environmentsById.get(environmentId));
    }

    @Override
    public Optional<Environment> findEnvironment(String projectId, String key) {
        return Optional.ofNullable(environmentIdsByProjectAndKey.get(environmentLookupKey(projectId, key)))
                .map(environmentsById::get);
    }

    @Override
    public List<Environment> listEnvironments(String projectId) {
        return environmentsById.values().stream()
                .filter(environment -> projectId.equals(environment.projectId()))
                .sorted(Comparator.comparing(Environment::key))
                .toList();
    }

    private static String environmentLookupKey(String projectId, String key) {
        return projectId + ":" + key;
    }
}
