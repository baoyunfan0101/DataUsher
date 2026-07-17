package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIntegrationErrorMapperTest {
    private final DefaultIntegrationErrorMapper mapper = new DefaultIntegrationErrorMapper();

    @Test
    void mapsWrappedAndExplicitFailuresToStableCodes() {
        var timeout = mapper.map(
                "mysql", new CompletionException(new TimeoutException("vendor detail")));
        var rateLimit = mapper.map("llm", new ExternalSystemException(
                IntegrationErrorCode.RATE_LIMITED,
                "provider quota exceeded",
                true,
                Map.of("providerCode", "429"),
                null
        ));

        assertEquals(IntegrationErrorCode.TIMEOUT, timeout.code());
        assertEquals("external system call timed out", timeout.message());
        assertTrue(timeout.retryable());
        assertEquals(IntegrationErrorCode.RATE_LIMITED, rateLimit.code());
        assertEquals("429", rateLimit.details().get("providerCode"));
    }

    @Test
    void hidesUnknownFailureMessagesAndDoesNotSwallowFatalErrors() {
        var unknown = mapper.map("mysql", new RuntimeException("password=secret"));

        assertEquals(IntegrationErrorCode.EXTERNAL_FAILURE, unknown.code());
        assertEquals("external system call failed", unknown.message());
        assertThrows(AssertionError.class,
                () -> mapper.map("mysql", new AssertionError("fatal")));
    }
}
