package com.datausher.data.datasource.api;

public interface DatasourceDiscoveryService {
    DatasourceConnectionTest testConnection(TestDatasourceConnectionRequest request);

    DatasourceDiscoverySnapshot discover(DiscoverDatasourceRequest request);
}
