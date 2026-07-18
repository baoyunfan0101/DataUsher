package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceId;

import java.util.List;
import java.util.Optional;

public interface DatasourceStore {
    void create(DatasourceDefinition definition);

    void update(DatasourceDefinition expected, DatasourceDefinition replacement);

    Optional<DatasourceDefinition> find(DatasourceId datasourceId);

    List<DatasourceDefinition> list();
}
