package com.datausher.data.metadata.core;

import com.datausher.data.datasource.api.DatasourceId;
import com.datausher.data.metadata.api.MetadataAssetType;
import com.datausher.data.metadata.api.MetadataId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Sha256MetadataIdFactory implements MetadataIdFactory {
    @Override
    public MetadataId create(
            MetadataAssetType assetType,
            DatasourceId datasourceId,
            String externalId
    ) {
        Objects.requireNonNull(assetType, "assetType must not be null");
        Objects.requireNonNull(datasourceId, "datasourceId must not be null");
        String normalizedExternalId = Objects.requireNonNull(
                externalId, "externalId must not be null").trim();
        if (normalizedExternalId.isEmpty()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        String canonical = assetType.value() + "\u0000" + datasourceId.value()
                + "\u0000" + normalizedExternalId;
        return new MetadataId(
                assetType.value()
                        + "-" + HexFormat.of().formatHex(digest(canonical))
        );
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
