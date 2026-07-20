package com.datausher.integration.runtime.api;

import java.util.function.Supplier;

public interface AdapterInvocationExecutor {
    <T> T execute(
            AdapterRequestContext context,
            IntegrationAdapter adapter,
            String operation,
            Supplier<T> invocation
    );

    default <T> T execute(
            AdapterRequestContext context,
            IntegrationAdapter adapter,
            AdapterOperation operation,
            Supplier<T> invocation
    ) {
        return execute(context, adapter, operation.name(), invocation);
    }

    default void execute(
            AdapterRequestContext context,
            IntegrationAdapter adapter,
            String operation,
            Runnable invocation
    ) {
        execute(context, adapter, operation, () -> {
            invocation.run();
            return null;
        });
    }

    default void execute(
            AdapterRequestContext context,
            IntegrationAdapter adapter,
            AdapterOperation operation,
            Runnable invocation
    ) {
        execute(context, adapter, operation.name(), invocation);
    }
}
