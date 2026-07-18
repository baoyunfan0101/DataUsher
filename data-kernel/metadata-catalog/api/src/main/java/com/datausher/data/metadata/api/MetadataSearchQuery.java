package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceId;

import java.util.Set;

public record MetadataSearchQuery(
        String text,
        DatasourceId datasourceId,
        Set<MetadataAssetType> types
) {
    public MetadataSearchQuery {
        text = MetadataValues.requireText(text, "text");
        types = types == null || types.isEmpty()
                ? Set.of(MetadataAssetType.values())
                : Set.copyOf(types);
    }
}
