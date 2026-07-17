package com.datausher.governance.project.api;

import java.util.List;
import java.util.Optional;

public interface EnvironmentQueryService {
    Optional<Environment> findEnvironmentById(String environmentId);

    Optional<Environment> findByProjectAndKey(String projectId, String key);

    List<Environment> listByProject(String projectId);
}
