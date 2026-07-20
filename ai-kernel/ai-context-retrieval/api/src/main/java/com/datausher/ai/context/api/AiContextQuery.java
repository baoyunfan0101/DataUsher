package com.datausher.ai.context.api;

import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.resource.api.ResourceRef;
import com.datausher.platform.shared.context.RequestContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AiContextQuery(
        String queryText,
        Set<SubjectRef> subjects,
        Set<ResourceRef> resources,
        Set<AiContextSourceType> sourceTypes,
        int maxItems,
        Map<String, String> attributes,
        RequestContext requestContext
) {
    public AiContextQuery {
        queryText = AiContextValues.optionalText(queryText);
        subjects = subjects == null ? Set.of() : Set.copyOf(subjects);
        resources = resources == null ? Set.of() : Set.copyOf(resources);
        sourceTypes = sourceTypes == null ? Set.of() : Set.copyOf(sourceTypes);
        attributes = AiContextValues.attributes(attributes);
        requestContext = Objects.requireNonNull(
                requestContext, "requestContext must not be null");
        if (new HashSet<>(subjects).size() != subjects.size()) {
            throw new IllegalArgumentException("subjects must not contain duplicates");
        }
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException("subjects must not be empty");
        }
        if (maxItems < 1 || maxItems > 100) {
            throw new IllegalArgumentException("maxItems must be 1..100");
        }
    }
}
