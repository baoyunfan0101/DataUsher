package com.datausher.ai.context.core;

import com.datausher.ai.context.api.AiContextItem;
import com.datausher.ai.context.api.AiContextQuery;
import com.datausher.ai.context.api.AiContextQueryService;
import com.datausher.ai.context.api.AiContextResult;
import com.datausher.governance.access.api.AccessDecisionService;
import com.datausher.governance.access.api.AccessRequest;
import com.datausher.governance.resource.api.ResourceRef;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultAiContextQueryService implements AiContextQueryService {
    private final List<AiContextProvider> providers;
    private final AccessDecisionService accessDecisions;

    public DefaultAiContextQueryService(
            List<AiContextProvider> providers,
            AccessDecisionService accessDecisions
    ) {
        this.providers = List.copyOf(Objects.requireNonNull(
                providers, "providers must not be null"));
        this.accessDecisions = Objects.requireNonNull(
                accessDecisions, "accessDecisions must not be null");
    }

    @Override
    public AiContextResult search(AiContextQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<AiContextItem> matches = providers.stream()
                .filter(provider -> query.sourceTypes().isEmpty()
                        || query.sourceTypes().contains(provider.sourceType()))
                .flatMap(provider -> provider.search(query).items().stream())
                .filter(item -> matchesResourceFilter(item, query))
                .filter(item -> allowed(item, query))
                .sorted(Comparator.comparingInt(AiContextItem::priority).reversed()
                        .thenComparing(AiContextItem::title))
                .limit(query.maxItems() + 1L)
                .toList();
        boolean truncated = matches.size() > query.maxItems();
        List<AiContextItem> items = truncated
                ? matches.subList(0, query.maxItems()) : matches;
        return new AiContextResult(items, truncated, Map.of());
    }

    private static boolean matchesResourceFilter(AiContextItem item, AiContextQuery query) {
        if (query.resources().isEmpty()) {
            return true;
        }
        Optional<ResourceRef> resource = item.source().resource();
        return resource.isPresent() && query.resources().contains(resource.orElseThrow());
    }

    private boolean allowed(AiContextItem item, AiContextQuery query) {
        Optional<ResourceRef> resource = item.source().resource();
        if (resource.isEmpty()) {
            return true;
        }
        return accessDecisions.decide(new AccessRequest(
                query.subjects(), "read", resource.orElseThrow(),
                query.requestContext(), Map.of("sourceType", item.source().type().value())))
                .allowed();
    }
}
