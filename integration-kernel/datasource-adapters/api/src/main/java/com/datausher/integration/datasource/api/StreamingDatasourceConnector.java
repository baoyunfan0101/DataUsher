package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;

public interface StreamingDatasourceConnector extends DatasourceConnector {
    StreamBatch read(AdapterRequestContext context, StreamReadRequest request);
}
