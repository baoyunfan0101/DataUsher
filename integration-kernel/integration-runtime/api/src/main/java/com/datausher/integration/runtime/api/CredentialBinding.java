package com.datausher.integration.runtime.api;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public record CredentialBinding(
        String bindingId,
        String adapterId,
        URI credentialReference,
        long revision,
        Map<String, String> attributes
) {
    public CredentialBinding {
        bindingId = IntegrationIdentifiers.normalize(bindingId, "bindingId");
        adapterId = IntegrationIdentifiers.normalize(adapterId, "adapterId");
        credentialReference = Objects.requireNonNull(
                credentialReference, "credentialReference must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (!credentialReference.isAbsolute()
                || credentialReference.getUserInfo() != null
                || credentialReference.getQuery() != null) {
            throw new IllegalArgumentException(
                    "credentialReference must be an absolute URI without user information or query");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }
}
