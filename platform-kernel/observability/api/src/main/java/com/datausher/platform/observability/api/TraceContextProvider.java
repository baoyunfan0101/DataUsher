package com.datausher.platform.observability.api;

public interface TraceContextProvider {
    TraceContext currentTraceContext();
}
