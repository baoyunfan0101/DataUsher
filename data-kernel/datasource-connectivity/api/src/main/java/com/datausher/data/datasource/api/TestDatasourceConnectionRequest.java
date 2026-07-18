package com.datausher.data.datasource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record TestDatasourceConnectionRequest(
        DatasourceId datasourceId,
        RequestContext requestContext
) {
    public TestDatasourceConnectionRequest {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
