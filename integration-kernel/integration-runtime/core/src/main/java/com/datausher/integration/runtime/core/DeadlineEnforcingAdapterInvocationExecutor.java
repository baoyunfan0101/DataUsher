package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.AdapterInvocationExecutor;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationAdapter;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class DeadlineEnforcingAdapterInvocationExecutor
        implements AdapterInvocationExecutor {
    private final ExecutorService executorService;
    private final Clock clock;

    public DeadlineEnforcingAdapterInvocationExecutor(
            ExecutorService executorService,
            Clock clock
    ) {
        this.executorService = Objects.requireNonNull(
                executorService, "executorService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public <T> T execute(
            AdapterRequestContext context,
            IntegrationAdapter adapter,
            String operation,
            Supplier<T> invocation
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(adapter, "adapter must not be null");
        String normalizedOperation = IntegrationIdentifiers.normalize(operation, "operation");
        Objects.requireNonNull(invocation, "invocation must not be null");
        String adapterId = Objects.requireNonNull(
                adapter.descriptor(), "adapter descriptor must not be null").adapterId();
        Duration remaining = Duration.between(clock.instant(), context.deadline());
        if (remaining.isZero() || remaining.isNegative()) {
            throw timeout(adapterId, normalizedOperation, "adapter request deadline has expired");
        }

        Future<T> future = executorService.submit(invocation::get);
        try {
            return future.get(timeoutNanos(remaining), TimeUnit.NANOSECONDS);
        } catch (TimeoutException failure) {
            future.cancel(true);
            throw timeout(adapterId, normalizedOperation, "adapter invocation timed out");
        } catch (InterruptedException failure) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw cancelled(adapterId, normalizedOperation, failure);
        } catch (CancellationException failure) {
            throw cancelled(adapterId, normalizedOperation, failure);
        } catch (ExecutionException failure) {
            throw propagate(failure.getCause(), adapterId, normalizedOperation);
        }
    }

    private static long timeoutNanos(Duration remaining) {
        try {
            return remaining.toNanos();
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static RuntimeException propagate(
            Throwable failure,
            String adapterId,
            String operation
    ) {
        if (failure instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        return new ExternalSystemException(
                IntegrationErrorCode.EXTERNAL_FAILURE,
                "adapter invocation failed",
                false,
                details(adapterId, operation),
                failure
        );
    }

    private static ExternalSystemException timeout(
            String adapterId,
            String operation,
            String message
    ) {
        return new ExternalSystemException(
                IntegrationErrorCode.TIMEOUT,
                message,
                true,
                details(adapterId, operation),
                null
        );
    }

    private static ExternalSystemException cancelled(
            String adapterId,
            String operation,
            Throwable failure
    ) {
        return new ExternalSystemException(
                IntegrationErrorCode.CANCELLED,
                "adapter invocation was cancelled",
                false,
                details(adapterId, operation),
                failure
        );
    }

    private static Map<String, String> details(String adapterId, String operation) {
        return Map.of("adapterId", adapterId, "operation", operation);
    }
}
