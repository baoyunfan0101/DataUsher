package com.datausher.ai.runtime.api;

public final class AiRuntimeEvents {
    public static final String TOOL_INVOCATION_STARTED = "ai-runtime.tool-invocation-started";
    public static final String TOOL_INVOCATION_COMPLETED = "ai-runtime.tool-invocation-completed";
    public static final String PROVIDER_CALLED = "ai-runtime.provider-called";

    private AiRuntimeEvents() {
    }
}
