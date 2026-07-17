package com.datausher.governance.resource.api;

public record ResourceQuery(
        String resourceType,
        ResourceScope scope,
        ResourceLifecycle lifecycle
) {
    public ResourceQuery {
        if (resourceType != null) {
            resourceType = new ResourceRef(resourceType, "validation", ResourceScope.global()).resourceType();
        }
    }

    public static ResourceQuery all() {
        return new ResourceQuery(null, null, null);
    }
}
