package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.IntegrationAdapter;

import java.util.List;

public interface DatasourceConnector extends IntegrationAdapter {
    ConnectionTestResult testConnection(
            AdapterRequestContext context,
            DatasourceConnection connection
    );

    List<DatasourceObject> discover(AdapterRequestContext context, DiscoveryRequest request);
}
