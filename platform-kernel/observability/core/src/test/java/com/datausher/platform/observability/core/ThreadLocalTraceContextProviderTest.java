package com.datausher.platform.observability.core;

import com.datausher.platform.observability.api.TraceContext;
import com.datausher.platform.observability.api.TraceContextManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThreadLocalTraceContextProviderTest {
    @Test
    void restoresNestedContextInReverseOrder() {
        ThreadLocalTraceContextProvider provider = new ThreadLocalTraceContextProvider();
        TraceContext outer = context("trace-outer", "span-outer");
        TraceContext inner = context("trace-inner", "span-inner");

        assertSame(TraceContext.EMPTY, provider.currentTraceContext());
        try (TraceContextManager.Scope ignored = provider.attach(outer)) {
            assertSame(outer, provider.currentTraceContext());
            try (TraceContextManager.Scope nested = provider.attach(inner)) {
                assertSame(inner, provider.currentTraceContext());
            }
            assertSame(outer, provider.currentTraceContext());
        }
        assertSame(TraceContext.EMPTY, provider.currentTraceContext());
    }

    @Test
    void keepsProviderInstancesIsolated() {
        ThreadLocalTraceContextProvider first = new ThreadLocalTraceContextProvider();
        ThreadLocalTraceContextProvider second = new ThreadLocalTraceContextProvider();

        try (TraceContextManager.Scope ignored = first.attach(context("trace", "span"))) {
            assertSame(TraceContext.EMPTY, second.currentTraceContext());
        }
    }

    @Test
    void rejectsOutOfOrderScopeClosureWithoutLosingContext() {
        ThreadLocalTraceContextProvider provider = new ThreadLocalTraceContextProvider();
        TraceContext outer = context("trace-outer", "span-outer");
        TraceContext inner = context("trace-inner", "span-inner");
        TraceContextManager.Scope outerScope = provider.attach(outer);
        TraceContextManager.Scope innerScope = provider.attach(inner);

        assertThrows(IllegalStateException.class, outerScope::close);
        assertSame(inner, provider.currentTraceContext());

        innerScope.close();
        outerScope.close();
        assertSame(TraceContext.EMPTY, provider.currentTraceContext());
    }

    @Test
    void rejectsClosureFromAnotherThread() throws InterruptedException {
        ThreadLocalTraceContextProvider provider = new ThreadLocalTraceContextProvider();
        TraceContext context = context("trace", "span");
        TraceContextManager.Scope scope = provider.attach(context);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                scope.close();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });

        thread.start();
        thread.join();

        assertSame(IllegalStateException.class, failure.get().getClass());
        assertSame(context, provider.currentTraceContext());
        scope.close();
        assertSame(TraceContext.EMPTY, provider.currentTraceContext());
    }

    private static TraceContext context(String traceId, String spanId) {
        return new TraceContext(traceId, spanId, Map.of());
    }
}
