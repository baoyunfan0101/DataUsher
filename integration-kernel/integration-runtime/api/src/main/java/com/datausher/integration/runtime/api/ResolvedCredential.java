package com.datausher.integration.runtime.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ResolvedCredential(
        String bindingId,
        String adapterId,
        long revision,
        Map<String, SecretString> secrets,
        Map<String, String> attributes
) {
    public ResolvedCredential {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
        secrets = copySecrets(secrets);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ResolvedCredential of(
            CredentialBinding binding,
            Map<String, SecretString> secrets,
            Map<String, String> attributes
    ) {
        Objects.requireNonNull(binding, "binding must not be null");
        return new ResolvedCredential(
                binding.bindingId(),
                binding.adapterId(),
                binding.revision(),
                secrets,
                attributes
        );
    }

    private static Map<String, SecretString> copySecrets(Map<String, SecretString> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("secrets must not be empty");
        }
        Map<String, SecretString> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(
                IntegrationIdentifiers.normalize(key, "secret key"),
                Objects.requireNonNull(value, "secret value must not be null")
        ));
        return Map.copyOf(copy);
    }
}
