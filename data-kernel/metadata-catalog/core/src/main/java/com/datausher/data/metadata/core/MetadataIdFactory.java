package com.datausher.data.metadata.core;

import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.metadata.api.MetadataAssetType;
import com.datausher.data.metadata.api.MetadataId;

public interface MetadataIdFactory {
    MetadataId create(
            MetadataAssetType assetType,
            DatasourceId datasourceId,
            String externalId
    );
}
