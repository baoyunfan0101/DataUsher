package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.datasource.api.DatasourceQuery;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryDatasourceStore implements DatasourceStore {
    private final ConcurrentMap<DatasourceId, DatasourceDefinition> definitions =
            new ConcurrentHashMap<>();

    @Override
    public void create(DatasourceDefinition definition) {
        DatasourceDefinition existing = definitions.putIfAbsent(
                definition.datasourceId(), definition);
        if (existing != null) {
            throw new IllegalStateException(
                    "datasource already exists: " + definition.datasourceId());
        }
    }

    @Override
    public void update(DatasourceDefinition expected, DatasourceDefinition replacement) {
        if (!expected.datasourceId().equals(replacement.datasourceId())) {
            throw new IllegalArgumentException("datasource IDs must match");
        }
        if (!definitions.replace(expected.datasourceId(), expected, replacement)) {
            throw new IllegalStateException(
                    "datasource changed concurrently: " + expected.datasourceId());
        }
    }

    @Override
    public Optional<DatasourceDefinition> find(DatasourceId datasourceId) {
        return Optional.ofNullable(definitions.get(datasourceId));
    }

    @Override
    public PageResult<DatasourceDefinition> search(
            DatasourceQuery query,
            PageRequest pageRequest
    ) {
        String searchText = query.text() == null
                ? null
                : query.text().toLowerCase(Locale.ROOT);
        var matches = definitions.values().stream()
                .filter(definition -> query.adapterId() == null
                        || definition.adapterId().equals(query.adapterId()))
                .filter(definition -> query.status() == null
                        || definition.status() == query.status())
                .filter(definition -> searchText == null
                        || definition.datasourceId().value().contains(searchText)
                        || definition.displayName().toLowerCase(Locale.ROOT).contains(searchText))
                .sorted(Comparator.comparing(DatasourceDefinition::datasourceId))
                .toList();
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = Math.min(fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }
}
