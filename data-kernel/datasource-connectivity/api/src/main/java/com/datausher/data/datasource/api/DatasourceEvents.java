package com.datausher.data.datasource.api;

public final class DatasourceEvents {
    public static final String REGISTERED = "DatasourceRegistered";
    public static final String STATUS_CHANGED = "DatasourceStatusChanged";
    public static final String CONNECTION_TESTED = "DatasourceConnectionTested";
    public static final String METADATA_DISCOVERED = "DatasourceMetadataDiscovered";

    private DatasourceEvents() {
    }
}
