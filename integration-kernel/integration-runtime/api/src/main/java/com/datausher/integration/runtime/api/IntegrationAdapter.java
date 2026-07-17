package com.datausher.integration.runtime.api;

public interface IntegrationAdapter {
    AdapterDescriptor descriptor();

    AdapterHealth checkHealth();
}
