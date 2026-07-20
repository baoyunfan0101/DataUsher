package com.datausher.data.quality.core;

import com.datausher.execution.api.ExecutionWorkloadType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProfileResultDecoderRegistry {
    private final Map<ExecutionWorkloadType, ProfileResultDecoder> decoders;

    public ProfileResultDecoderRegistry(Collection<? extends ProfileResultDecoder> decoders) {
        Map<ExecutionWorkloadType, ProfileResultDecoder> indexed = new HashMap<>();
        for (ProfileResultDecoder decoder : Objects.requireNonNull(
                decoders, "decoders must not be null")) {
            ProfileResultDecoder existing = indexed.putIfAbsent(
                    decoder.workloadType(), decoder);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "duplicate profile result decoder: " + decoder.workloadType().value());
            }
        }
        this.decoders = Map.copyOf(indexed);
    }

    public ProfileResultDecoder require(ExecutionWorkloadType type) {
        ProfileResultDecoder decoder = decoders.get(
                Objects.requireNonNull(type, "type must not be null"));
        if (decoder == null) {
            throw new IllegalStateException(
                    "profile result workload is not supported: " + type.value());
        }
        return decoder;
    }
}
