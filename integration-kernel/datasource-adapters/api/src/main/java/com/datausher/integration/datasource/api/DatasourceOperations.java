package com.datausher.integration.datasource.api;

import com.datausher.integration.runtime.api.AdapterOperation;

public final class DatasourceOperations {
    public static final AdapterOperation TEST_CONNECTION = AdapterOperation.of(
            "datasource.connection.test", DatasourceCapabilities.DISCOVERY, false);
    public static final AdapterOperation DISCOVER = AdapterOperation.of(
            "datasource.discover", DatasourceCapabilities.DISCOVERY, false);

    private DatasourceOperations() {
    }
}
