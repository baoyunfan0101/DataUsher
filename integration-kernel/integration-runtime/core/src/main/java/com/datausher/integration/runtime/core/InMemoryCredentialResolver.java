package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.CredentialBinding;
import com.datausher.integration.runtime.api.CredentialResolver;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;
import com.datausher.integration.runtime.api.ResolvedCredential;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCredentialResolver implements CredentialResolver {
    private final ConcurrentMap<String, ResolvedCredential> credentials = new ConcurrentHashMap<>();

    public ResolvedCredential put(ResolvedCredential credential) {
        Objects.requireNonNull(credential, "credential must not be null");
        credentials.put(credential.bindingId(), credential);
        return credential;
    }

    public Optional<ResolvedCredential> remove(String bindingId) {
        return Optional.ofNullable(credentials.remove(normalize(bindingId, "bindingId")));
    }

    @Override
    public ResolvedCredential resolve(AdapterRequestContext context, CredentialBinding binding) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(binding, "binding must not be null");
        ResolvedCredential credential = credentials.get(binding.bindingId());
        if (credential == null) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.NOT_FOUND,
                    "credential binding is not resolved: " + binding.bindingId(),
                    false,
                    Map.of("bindingId", binding.bindingId()),
                    null
            );
        }
        if (!credential.adapterId().equals(binding.adapterId())
                || credential.revision() != binding.revision()) {
            throw new ExternalSystemException(
                    IntegrationErrorCode.CONFLICT,
                    "resolved credential does not match binding revision: " + binding.bindingId(),
                    false,
                    Map.of("bindingId", binding.bindingId()),
                    null
            );
        }
        return credential;
    }

    private static String normalize(String value, String fieldName) {
        return IntegrationIdentifiers.normalize(value, fieldName);
    }
}
