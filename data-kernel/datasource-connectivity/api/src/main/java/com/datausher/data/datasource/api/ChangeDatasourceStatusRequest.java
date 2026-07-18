package com.datausher.data.datasource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record ChangeDatasourceStatusRequest(
        DatasourceId datasourceId,
        DatasourceStatus status,
        long expectedRevision,
        RequestContext requestContext
) {
    public ChangeDatasourceStatusRequest {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (expectedRevision < 1) {
            throw new IllegalArgumentException("expectedRevision must be greater than zero");
        }
    }
}
