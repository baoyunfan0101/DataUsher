package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DatasourceQuery;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface DatasourceStore {
    void create(DatasourceDefinition definition);

    void update(DatasourceDefinition expected, DatasourceDefinition replacement);

    Optional<DatasourceDefinition> find(DatasourceId datasourceId);

    PageResult<DatasourceDefinition> search(DatasourceQuery query, PageRequest pageRequest);
}
