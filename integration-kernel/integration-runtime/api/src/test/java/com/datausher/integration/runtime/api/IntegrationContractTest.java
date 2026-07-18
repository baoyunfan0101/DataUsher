package com.datausher.integration.runtime.api;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

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
}
