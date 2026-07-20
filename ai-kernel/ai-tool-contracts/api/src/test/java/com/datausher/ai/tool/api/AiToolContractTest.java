package com.datausher.ai.tool.api;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.governance.resource.api.ResourceRef;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiToolContractTest {
    @Test
    void keepsToolParameterTypesOpenForExtension() {
        assertEquals("semantic-id", new AiToolParameterType("semantic-id").value());
    }

    @Test
    void rejectsDuplicateParameterNames() {
        AiToolParameter parameter = new AiToolParameter(
                "query", AiToolParameterType.STRING, true, "", Map.of());

        assertThrows(IllegalArgumentException.class, () -> new AiToolSchema(
                new AiToolRef(new AiToolId("catalog.search"), 1),
                "Catalog search", "", List.of(parameter, parameter),
                AiToolStatus.ACTIVE, Map.of(), 1));
    }

    @Test
    void modelsPermissionRequirementsAndTypedResults() {
        AiToolPermissionRequirement requirement = new AiToolPermissionRequirement(
                ResourceRef.global("catalog", "orders"),
                java.util.Set.of("read"), Map.of());
        AiToolResult result = new AiToolResult(
                new AiToolRef(new AiToolId("catalog.search"), 1),
                AiToolResultStatus.SUCCEEDED,
                Map.of("count", new ExecutionValue.DecimalValue(12)),
                "ok", Map.of(), Instant.EPOCH);

        assertEquals("catalog", requirement.resource().resourceType());
        assertEquals(new ExecutionValue.DecimalValue(12), result.values().get("count"));
    }
}
