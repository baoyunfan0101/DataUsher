package com.datausher.ai.tool.api;

import java.util.Objects;

public record AiToolRef(AiToolId toolId, long version) {
    public AiToolRef {
        toolId = Objects.requireNonNull(toolId, "toolId must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }
}
