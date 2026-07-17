package com.datausher.governance.project.api;

public interface ProjectCommandService {
    Project create(CreateProjectRequest request);

    Project changeStatus(ChangeProjectStatusRequest request);
}
