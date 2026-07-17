package com.datausher.governance.project.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface ProjectQueryService {
    Optional<Project> findProjectById(String projectId);

    Optional<Project> findByKey(String key);

    PageResult<Project> list(PageRequest pageRequest);
}
