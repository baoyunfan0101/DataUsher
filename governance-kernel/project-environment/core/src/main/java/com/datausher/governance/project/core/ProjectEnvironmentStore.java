package com.datausher.governance.project.core;

import com.datausher.governance.project.api.Environment;
import com.datausher.governance.project.api.Project;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;
import java.util.Optional;

public interface ProjectEnvironmentStore {
    void create(Project project, List<Environment> environments);

    void delete(Project project, List<Environment> environments);

    void update(Project expected, Project replacement);

    Optional<Project> findProjectById(String projectId);

    Optional<Project> findProjectByKey(String key);

    PageResult<Project> listProjects(PageRequest pageRequest);

    Optional<Environment> findEnvironmentById(String environmentId);

    Optional<Environment> findEnvironment(String projectId, String key);

    List<Environment> listEnvironments(String projectId);
}
