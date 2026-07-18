package com.datausher.integration.contract;

import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationAdapter;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IntegrationAdapterContract {
    private IntegrationAdapterContract() {
    }

    public static void verify(IntegrationAdapter adapter, Set<String> sensitiveValues) {
        IntegrationAdapter checkedAdapter = Objects.requireNonNull(
                adapter, "adapter must not be null");
        Set<String> checkedSensitiveValues = immutableSensitiveValues(sensitiveValues);
        AdapterDescriptor first = checkedAdapter.descriptor();
        AdapterDescriptor second = checkedAdapter.descriptor();
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second, "adapter descriptor must remain stable");
        assertFalse(first.capabilities().isEmpty(), "adapter must declare capabilities");

        AdapterHealth health = checkedAdapter.checkHealth();
        assertNotNull(health);
        assertEquals(first.adapterId(), health.adapterId(),
                "health must use the descriptor adapter ID");
        assertSafe(health.message(), health.details(), checkedSensitiveValues);
        assertThrows(UnsupportedOperationException.class,
                () -> health.details().put("contract", "mutation"),
                "health details must be immutable");
    }

    public static ExternalSystemException verifyMappedFailure(
            Throwable failure,
            Set<String> sensitiveValues
    ) {
        assertNotNull(failure, "failure must not be null");
        assertTrue(failure instanceof ExternalSystemException,
                "external failures must be mapped to ExternalSystemException");
        ExternalSystemException mapped = (ExternalSystemException) failure;
        assertSafe(mapped.getMessage(), mapped.details(),
                immutableSensitiveValues(sensitiveValues));
        if (mapped.getCause() instanceof Error error) {
            assertSame(error, mapped.getCause());
        }
        return mapped;
    }

    static void assertSafe(
            String message,
            Map<String, String> details,
            Set<String> sensitiveValues
    ) {
        for (String sensitiveValue : sensitiveValues) {
            assertFalse(message.contains(sensitiveValue),
                    "message must not expose a sensitive value");
            assertTrue(details.values().stream()
                            .noneMatch(value -> value.contains(sensitiveValue)),
                    "details must not expose a sensitive value");
        }
    }

    private static Set<String> immutableSensitiveValues(Set<String> sensitiveValues) {
        Set<String> values = sensitiveValues == null ? Set.of() : Set.copyOf(sensitiveValues);
        assertTrue(values.stream().noneMatch(String::isBlank),
                "sensitive values must not be blank");
        return values;
    }
}
