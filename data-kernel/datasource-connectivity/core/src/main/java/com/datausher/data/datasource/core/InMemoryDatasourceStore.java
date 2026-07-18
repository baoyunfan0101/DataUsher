package com.datausher.data.datasource.core;

import com.datausher.data.datasource.api.DatasourceDefinition;
import com.datausher.data.datasource.api.DatasourceId;

import java.util.Comparator;
import java.util.List;
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
    public List<DatasourceDefinition> list() {
        return definitions.values().stream()
                .sorted(Comparator.comparing(DatasourceDefinition::datasourceId))
                .toList();
    }
}
