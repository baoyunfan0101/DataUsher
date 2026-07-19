package com.datausher.data.lineage.api;

import java.util.Map;
import java.util.Objects;

public record LineageNodeInput(
        LineageNodeRef reference,
        String displayName,
        Map<String, String> attributes
) {
    public LineageNodeInput {
        reference = Objects.requireNonNull(reference, "reference must not be null");
        displayName = LineageValues.text(displayName, "displayName");
        attributes = LineageValues.attributes(attributes);
    }
}
