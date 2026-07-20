package com.datausher.integration.runtime.api;

public interface CredentialResolver {
    ResolvedCredential resolve(AdapterRequestContext context, CredentialBinding binding);
}
