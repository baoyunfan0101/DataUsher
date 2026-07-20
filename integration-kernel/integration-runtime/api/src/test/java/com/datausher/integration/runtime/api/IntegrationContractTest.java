package com.datausher.integration.runtime.api;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationContractTest {
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Test
    void requiresEveryAdapterToDeclareAtLeastOneCapability() {
        assertThrows(IllegalArgumentException.class, () -> new AdapterDescriptor(
                "mysql", AdapterType.DATASOURCE, "1.0", Set.of(), Map.of()));
    }

    @Test
    void rejectsDuplicateCapabilityNamesEvenWhenAttributesDiffer() {
        assertThrows(IllegalArgumentException.class, () -> new AdapterDescriptor(
                "mysql",
                AdapterType.DATASOURCE,
                "1.0",
                Set.of(
                        new AdapterCapability("datasource.discovery", Map.of("mode", "full")),
                        new AdapterCapability("datasource.discovery", Map.of("mode", "partial"))
                ),
                Map.of()
        ));
    }

    @Test
    void evaluatesRequestDeadlineAgainstAnInjectedClock() {
        AdapterRequestContext context = new AdapterRequestContext(
                "request-1", NOW, Map.of("traceId", "trace-1"));

        assertTrue(context.isExpired(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void protectsBinaryValuesFromCallerMutation() {
        byte[] source = new byte[] {1, 2, 3};
        IntegrationValue.BinaryValue value = IntegrationValue.BinaryValue.fromBytes(source);
        source[0] = 9;
        byte[] returned = value.bytes();
        returned[1] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, value.bytes());
    }

    @Test
    void rejectsCredentialReferencesThatContainUserInformation() {
        assertThrows(IllegalArgumentException.class, () -> new CredentialBinding(
                "analytics",
                "mysql",
                URI.create("https://user:secret@example.test/credential"),
                1,
                Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new CredentialBinding(
                "analytics",
                "mysql",
                URI.create("vault://data/mysql?token=secret"),
                1,
                Map.of()
        ));
    }

    @Test
    void redactsResolvedCredentialSecretsFromToString() {
        CredentialBinding binding = new CredentialBinding(
                "analytics", "mysql", URI.create("vault://data/mysql"), 1, Map.of());
        ResolvedCredential credential = ResolvedCredential.of(
                binding,
                Map.of("password", new SecretString("super-secret")),
                Map.of("host", "warehouse"));

        assertTrue(credential.toString().contains("[secret]"));
        assertTrue(!credential.toString().contains("super-secret"));
        assertThrows(UnsupportedOperationException.class,
                () -> credential.secrets().put("token", new SecretString("value")));
    }

    @Test
    void redactsSensitiveValuesFromMessagesAndDetails() {
        SensitiveValueRedactor redactor = SensitiveValueRedactor.of(Set.of(
                "secret-token", "secret"));

        assertTrue(!redactor.redact("token=secret-token").contains("secret-token"));
        assertTrue(!redactor.redact(Map.of("detail", "password=secret"))
                .get("detail")
                .contains("secret"));
        assertThrows(IllegalArgumentException.class,
                () -> SensitiveValueRedactor.of(Set.of("")));
    }

    @Test
    void namesAdapterOperationsWithCapabilitiesAndMutationSemantics() {
        AdapterOperation operation = AdapterOperation.of(
                "compute.job.submit", "compute.job.execute", true);

        assertEquals("compute.job.submit", operation.name());
        assertEquals("compute.job.execute", operation.capabilityName());
        assertTrue(operation.mutating());
    }
}
