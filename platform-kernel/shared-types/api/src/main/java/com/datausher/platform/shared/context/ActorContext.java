package com.datausher.platform.shared.context;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ActorContext(
        String actorId,
        String displayName,
        Set<String> subjectRefs,
        Map<String, String> attributes
) {
    public static final ActorContext SYSTEM =
            new ActorContext("system", "System", Set.of(), Map.of());

    public ActorContext {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null").trim();
        displayName = displayName == null ? actorId : displayName.trim();
        subjectRefs = subjectRefs == null ? Set.of() : Set.copyOf(subjectRefs);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        if (actorId.isEmpty()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        if (displayName.isEmpty()) {
            displayName = actorId;
        }
    }
}
