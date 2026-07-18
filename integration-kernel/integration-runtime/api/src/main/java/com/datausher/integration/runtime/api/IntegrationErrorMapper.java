package com.datausher.integration.runtime.api;

public interface IntegrationErrorMapper {
    IntegrationError map(String adapterId, Throwable failure);
}
