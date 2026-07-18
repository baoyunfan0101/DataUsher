package com.datausher.data.datasource.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface DatasourceQueryService {
    Optional<DatasourceDefinition> find(DatasourceId datasourceId);

    PageResult<DatasourceDefinition> search(DatasourceQuery query, PageRequest pageRequest);
}
