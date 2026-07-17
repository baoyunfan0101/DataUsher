package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.ExternalSystemException;
import com.datausher.integration.runtime.api.IntegrationError;
import com.datausher.integration.runtime.api.IntegrationErrorCode;
import com.datausher.integration.runtime.api.IntegrationErrorMapper;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public final class DefaultIntegrationErrorMapper implements IntegrationErrorMapper {
    @Override
    public IntegrationError map(String adapterId, Throwable failure) {
        Throwable root = unwrap(Objects.requireNonNull(failure, "failure must not be null"));
        if (root instanceof Error error) {
            throw error;
        }

        if (root instanceof ExternalSystemException external) {
            return new IntegrationError(
                    external.errorCode(), adapterId, external.getMessage(),
                    external.retryable(), external.details());
        }
        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return error(
                    IntegrationErrorCode.TIMEOUT,
                    adapterId,
                    "external system call timed out",
                    true,
                    root
            );
        }
        if (root instanceof ConnectException) {
            return error(
                    IntegrationErrorCode.UNAVAILABLE,
                    adapterId,
                    "external system is unavailable",
                    true,
                    root
            );
        }
        if (root instanceof CancellationException || root instanceof InterruptedException) {
            return error(
                    IntegrationErrorCode.CANCELLED,
                    adapterId,
                    "external system call was cancelled",
                    false,
                    root
            );
        }
        if (root instanceof SecurityException) {
            return error(
                    IntegrationErrorCode.AUTHORIZATION_FAILED,
                    adapterId,
                    "external system authorization failed",
                    false,
                    root
            );
        }
        if (root instanceof IllegalArgumentException) {
            return error(
                    IntegrationErrorCode.INVALID_REQUEST,
                    adapterId,
                    "external system rejected the request",
                    false,
                    root
            );
        }
        return error(
                IntegrationErrorCode.EXTERNAL_FAILURE,
                adapterId,
                "external system call failed",
                false,
                root
        );
    }

    private static IntegrationError error(
            IntegrationErrorCode code,
            String adapterId,
            String message,
            boolean retryable,
            Throwable failure
    ) {
        return new IntegrationError(
                code,
                adapterId,
                message,
                retryable,
                Map.of("failureType", failure.getClass().getName())
        );
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
