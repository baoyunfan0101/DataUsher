package com.datausher.data.datasource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record DiscoverDatasourceRequest(
        DatasourceId datasourceId,
        String namespace,
        Map<String, String> options,
        RequestContext requestContext
) {
    public DiscoverDatasourceRequest {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        namespace = namespace == null ? "" : namespace.trim();
        options = options == null ? Map.of() : Map.copyOf(options);
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
    }
}
