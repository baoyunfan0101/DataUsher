package com.datausher.data.metadata.api;

import com.datausher.data.datasource.api.DatasourceId;

import java.util.Map;
import java.util.Objects;

public record MetadataSearchHit(
        MetadataAssetType type,
        MetadataId assetId,
        DatasourceId datasourceId,
        String name,
        String qualifiedName,
        String description,
        double score,
        Map<String, String> attributes
) {
    public MetadataSearchHit {
        type = Objects.requireNonNull(type, "type must not be null");
        assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        datasourceId = Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        name = MetadataValues.requireText(name, "name");
        qualifiedName = MetadataValues.requireText(qualifiedName, "qualifiedName");
        description = MetadataValues.normalizeOptional(description);
        attributes = MetadataValues.copyAttributes(attributes);
        if (!Double.isFinite(score) || score < 0) {
            throw new IllegalArgumentException("score must be finite and non-negative");
        }
    }
}
