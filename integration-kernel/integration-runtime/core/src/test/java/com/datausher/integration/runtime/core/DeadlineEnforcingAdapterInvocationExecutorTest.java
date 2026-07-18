package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterCapability;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterHealthStatus;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlineEnforcingAdapterInvocationExecutorTest {
    private static final Instant NOW = Instant.parse("2026-07-17T13:00:00Z");

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DeadlineEnforcingAdapterInvocationExecutor executor =
            new DeadlineEnforcingAdapterInvocationExecutor(
                    executorService,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @AfterEach
    void shutDownExecutor() {
        executorService.shutdownNow();
    }

    @Test
    void rejectsExpiredRequestsWithoutInvokingTheAdapter() {
        AtomicBoolean invoked = new AtomicBoolean();

        ExternalSystemException failure = assertThrows(
                ExternalSystemException.class,
                () -> executor.execute(
                        context(NOW), adapter(), "datasource.query",
                        () -> invoked.compareAndSet(false, true)
                )
        );

        assertEquals(IntegrationErrorCode.TIMEOUT, failure.errorCode());
        assertFalse(invoked.get());
    }

    @Test
    void cancelsInvocationsThatExceedTheDeadline() throws InterruptedException {
        CountDownLatch interrupted = new CountDownLatch(1);

        ExternalSystemException failure = assertThrows(
                ExternalSystemException.class,
                () -> executor.execute(
                        context(NOW.plus(Duration.ofMillis(50))),
                        adapter(),
                        "datasource.query",
                        () -> {
                            try {
                                Thread.sleep(Duration.ofSeconds(5));
                                return "late";
                            } catch (InterruptedException interruptedFailure) {
                                interrupted.countDown();
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interruptedFailure);
                            }
                        }
                )
        );

        assertEquals(IntegrationErrorCode.TIMEOUT, failure.errorCode());
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
    }

    @Test
    void returnsResultsAndPreservesRuntimeFailures() {
        assertEquals("result", executor.execute(
                context(NOW.plusSeconds(1)), adapter(), "datasource.query", () -> "result"));

        IllegalStateException expected = new IllegalStateException("adapter failed");
        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        context(NOW.plusSeconds(1)),
                        adapter(),
                        "datasource.query",
                        () -> {
                            throw expected;
                        }
                )
        );
        assertSame(expected, actual);
    }

    @Test
    void supportsDeadlinesBeyondNanosecondRange() {
        assertEquals("result", executor.execute(
                context(Instant.MAX), adapter(), "datasource.query", () -> "result"));
    }

    private static AdapterRequestContext context(Instant deadline) {
        return new AdapterRequestContext("request-1", deadline, Map.of());
    }

    private static IntegrationAdapter adapter() {
        AdapterDescriptor descriptor = new AdapterDescriptor(
                "adapter-1",
                AdapterType.DATASOURCE,
                "1.0.0",
                Set.of(AdapterCapability.of("datasource.query")),
                Map.of()
        );
        return new IntegrationAdapter() {
            @Override
            public AdapterDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public AdapterHealth checkHealth() {
                return new AdapterHealth(
                        descriptor.adapterId(),
                        AdapterHealthStatus.UP,
                        NOW,
                        "",
                        Map.of()
                );
            }
        };
    }
}
