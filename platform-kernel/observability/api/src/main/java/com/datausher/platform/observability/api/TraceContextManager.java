package com.datausher.platform.observability.api;

public interface TraceContextManager extends TraceContextProvider {
    Scope attach(TraceContext traceContext);

    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
