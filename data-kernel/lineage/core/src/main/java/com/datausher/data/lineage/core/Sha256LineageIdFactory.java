package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageEdgeId;
import com.datausher.data.lineage.api.LineageEdgeInput;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageSourceRef;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256LineageIdFactory implements LineageIdFactory {
    @Override
    public LineageNodeId nodeId(LineageNodeRef reference) {
        return new LineageNodeId("node-" + digest(
                reference.type().value() + "\u0000" + reference.externalId()));
    }

    @Override
    public LineageEdgeId edgeId(LineageSourceRef source, LineageEdgeInput edge) {
        return new LineageEdgeId("edge-" + digest(
                source.type().value() + "\u0000" + source.sourceId() + "\u0000"
                        + edge.upstream().type().value() + "\u0000"
                        + edge.upstream().externalId() + "\u0000"
                        + edge.downstream().type().value() + "\u0000"
                        + edge.downstream().externalId() + "\u0000" + edge.type().value()));
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
