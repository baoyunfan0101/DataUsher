package com.datausher.data.quality.core;

import com.datausher.execution.api.ExecutionWorkloadType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class QualityResultDecoderRegistry {
    private final Map<ExecutionWorkloadType, QualityResultDecoder> decoders;

    public QualityResultDecoderRegistry(Collection<? extends QualityResultDecoder> decoders) {
        Map<ExecutionWorkloadType, QualityResultDecoder> indexed = new HashMap<>();
        for (QualityResultDecoder decoder : Objects.requireNonNull(
                decoders, "decoders must not be null")) {
            QualityResultDecoder existing = indexed.putIfAbsent(
                    decoder.workloadType(), decoder);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate quality result decoder: " + decoder.workloadType().value());
            }
        }
        this.decoders = Map.copyOf(indexed);
    }

    public QualityResultDecoder require(ExecutionWorkloadType type) {
        QualityResultDecoder decoder = decoders.get(
                Objects.requireNonNull(type, "type must not be null"));
        if (decoder == null) {
            throw new IllegalStateException(
                    "quality result workload is not supported: " + type.value());
        }
        return decoder;
    }
}
