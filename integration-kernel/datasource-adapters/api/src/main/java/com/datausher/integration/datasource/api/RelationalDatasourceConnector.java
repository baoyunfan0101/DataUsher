package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface RelationalDatasourceConnector extends DatasourceConnector {
    QueryResult executeQuery(AdapterRequestContext context, QueryRequest request);
}
