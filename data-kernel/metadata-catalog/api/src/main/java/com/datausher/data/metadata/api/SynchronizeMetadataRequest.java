package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceDiscoverySnapshot;
import com.datausher.platform.shared.context.RequestContext;

import java.util.Objects;

public record SynchronizeMetadataRequest(
        DatasourceDiscoverySnapshot discovery,
        String catalogName,
        MetadataSyncMode mode,
        RequestContext requestContext
) {
    public SynchronizeMetadataRequest {
        discovery = Objects.requireNonNull(discovery, "discovery must not be null");
        catalogName = MetadataValues.requireText(catalogName, "catalogName");
        mode = Objects.requireNonNull(mode, "mode must not be null");
        requestContext = Objects.requireNonNull(requestContext, "requestContext must not be null");
        if (mode == MetadataSyncMode.REPLACE && !discovery.namespace().isEmpty()) {
            throw new IllegalArgumentException(
                    "REPLACE synchronization requires a full discovery snapshot");
        }
    }
}
