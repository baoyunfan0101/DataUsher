package com.datausher.data.datasource.api;

import com.datausher.platform.shared.context.RequestContext;

import java.util.Map;
import java.util.Objects;

public record RegisterDatasourceRequest(
        DatasourceId datasourceId,
        String displayName,
        String adapterId,
        String credentialBindingId,
        Map<String, String> connectionProperties,
        RequestContext requestContext
) {
    public RegisterDatasourceRequest {
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        DatasourceDefinition validated = new DatasourceDefinition(
                datasourceId,
                displayName,
                adapterId,
                credentialBindingId,
                connectionProperties,
                DatasourceStatus.ACTIVE,
                requestContext.requestTime(),
                requestContext.requestTime(),
                1
        );
        displayName = validated.displayName();
        adapterId = validated.adapterId();
        credentialBindingId = validated.credentialBindingId();
        connectionProperties = validated.connectionProperties();
    }
}
