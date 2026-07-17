package com.datausher.platform.shared.id.core;

import com.datausher.platform.shared.id.GeneratedId;
import com.datausher.platform.shared.id.IdFormat;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;

import java.util.Objects;
import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {
    @Override
    public GeneratedId nextId(IdGenerationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new GeneratedId(
                UUID.randomUUID().toString(),
                IdFormat.UUID,
                request,
                java.util.Map.of()
        );
    }
}
