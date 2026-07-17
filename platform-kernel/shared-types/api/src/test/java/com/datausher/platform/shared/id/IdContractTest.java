package com.datausher.platform.shared.id;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdContractTest {
    @Test
    void generatedIdRequiresFormatAndRequestMetadata() {
        assertThrows(NullPointerException.class, () ->
                new GeneratedId("id-1", null, IdGenerationRequest.global(), Map.of())
        );
        assertThrows(NullPointerException.class, () ->
                new GeneratedId("id-1", IdFormat.UUID, null, Map.of())
        );
    }

    @Test
    void generationRequestUsesExplicitGlobalFactory() {
        assertThrows(NullPointerException.class, () ->
                new IdGenerationRequest(null, "entity", Map.of())
        );
        assertEquals("global", IdGenerationRequest.global().domain());
        assertEquals("default", IdGenerationRequest.global().entityType());
    }

    @Test
    void normalizesAndValidatesIdFormats() {
        assertEquals(IdFormat.UUID, IdFormat.of(" UUID "));
        assertEquals(IdFormat.of("vendor.uuid"), IdFormat.of(" VENDOR.UUID "));
        assertThrows(IllegalArgumentException.class, () -> IdFormat.of("vendor uuid"));
        assertThrows(IllegalArgumentException.class, () -> IdFormat.of("vendor..uuid"));
    }

}
