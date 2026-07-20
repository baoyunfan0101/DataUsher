package com.datausher.ai.tool.api;

import java.util.List;

public record AiToolPermissionPolicy(
        List<AiToolPermissionRequirement> requirements,
        boolean requireAll
) {
    public AiToolPermissionPolicy {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    public static AiToolPermissionPolicy none() {
        return new AiToolPermissionPolicy(List.of(), true);
    }
}
