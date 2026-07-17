package com.datausher.platform.shared.id.core;

import com.datausher.platform.shared.id.GeneratedId;
import com.datausher.platform.shared.id.IdFormat;
import com.datausher.platform.shared.id.IdGenerationRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidIdGeneratorTest {
    @Test
    void preservesRequestMetadata() {
        IdGenerationRequest request = IdGenerationRequest.of("catalog", "entry");

        GeneratedId generatedId = new UuidIdGenerator().nextId(request);

        assertEquals(IdFormat.UUID, generatedId.format());
        assertEquals(request, generatedId.request());
        assertTrue(generatedId.value().matches("[0-9a-f-]{36}"));
    }

    @Test
    void rejectsMissingRequest() {
        assertThrows(NullPointerException.class, () -> new UuidIdGenerator().nextId(null));
    }
}
