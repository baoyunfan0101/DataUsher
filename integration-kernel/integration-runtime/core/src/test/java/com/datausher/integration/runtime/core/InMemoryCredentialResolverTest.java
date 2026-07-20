package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.CredentialBinding;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.ResolvedCredential;
import com.datausher.integration.runtime.api.SecretString;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryCredentialResolverTest {
    @Test
    void resolvesSecretsByBindingRevisionWithoutLeakingThem() {
        CredentialBinding binding = binding(1);
        InMemoryCredentialResolver resolver = new InMemoryCredentialResolver();
        resolver.put(ResolvedCredential.of(
                binding,
                Map.of("password", new SecretString("super-secret")),
                Map.of()));

        ResolvedCredential credential = resolver.resolve(context(), binding);

        assertEquals("super-secret", credential.secrets().get("password").reveal());
        assertEquals("[secret]", credential.secrets().get("password").toString());
    }

    @Test
    void rejectsMissingOrStaleResolvedCredentials() {
        CredentialBinding binding = binding(2);
        InMemoryCredentialResolver resolver = new InMemoryCredentialResolver();

        ExternalSystemException missing = assertThrows(ExternalSystemException.class,
                () -> resolver.resolve(context(), binding));
        assertEquals(IntegrationErrorCode.NOT_FOUND, missing.errorCode());

        resolver.put(ResolvedCredential.of(
                binding(1),
                Map.of("password", new SecretString("super-secret")),
                Map.of()));
        ExternalSystemException stale = assertThrows(ExternalSystemException.class,
                () -> resolver.resolve(context(), binding));
        assertEquals(IntegrationErrorCode.CONFLICT, stale.errorCode());
    }

    private static CredentialBinding binding(long revision) {
        return new CredentialBinding(
                "analytics", "mysql", URI.create("vault://data/mysql"), revision, Map.of());
    }

    private static AdapterRequestContext context() {
        return new AdapterRequestContext(
                "request-1", Instant.parse("2026-07-20T00:00:00Z"), Map.of());
    }
}
