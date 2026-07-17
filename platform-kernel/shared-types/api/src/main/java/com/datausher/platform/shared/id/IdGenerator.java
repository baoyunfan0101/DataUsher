package com.datausher.platform.shared.id;

import java.util.Objects;

public interface IdGenerator {
    GeneratedId nextId(IdGenerationRequest request);

    default GeneratedId nextId() {
        return nextId(IdGenerationRequest.global());
    }

    default String nextIdValue(IdGenerationRequest request) {
        return Objects.requireNonNull(nextId(request), "generated ID must not be null").value();
    }

    default String nextIdValue() {
        return Objects.requireNonNull(nextId(), "generated ID must not be null").value();
    }
}
