package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Objects;

public record StreamReadRequest(
        DatasourceConnection connection,
        String stream,
        String offset,
        int maxRecords
) {
    public StreamReadRequest {
        connection = Objects.requireNonNull(connection, "connection must not be null");
        stream = IntegrationIdentifiers.requireText(stream, "stream");
        offset = offset == null ? "" : offset.trim();
        if (maxRecords < 1) {
            throw new IllegalArgumentException("maxRecords must be greater than zero");
        }
    }
}
