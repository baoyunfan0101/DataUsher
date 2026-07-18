package com.datausher.data.datasource.api;

public interface DatasourceCommandService {
    DatasourceDefinition register(RegisterDatasourceRequest request);

    DatasourceDefinition changeStatus(ChangeDatasourceStatusRequest request);
}
