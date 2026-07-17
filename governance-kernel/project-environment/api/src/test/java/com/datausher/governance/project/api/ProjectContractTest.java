package com.datausher.governance.project.api;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectContractTest {
    private static final RequestContext CONTEXT =
            RequestContext.system("request-1", Instant.parse("2026-07-17T00:00:00Z"));

    @Test
    void createRequestNormalizesKeysAndCopiesInputs() {
        List<EnvironmentSpec> environments = new ArrayList<>(
                List.of(new EnvironmentSpec("DEV", "Development")));
        Map<String, String> attributes = new HashMap<>(Map.of("owner", "data"));

        CreateProjectRequest request = new CreateProjectRequest(
                "Analytics", "Analytics", environments, attributes, CONTEXT);
        environments.add(new EnvironmentSpec("prod", "Production"));
        attributes.put("owner", "changed");

        assertEquals("analytics", request.key());
        assertEquals(1, request.environments().size());
        assertEquals("data", request.attributes().get("owner"));
    }

    @Test
    void createRequestRejectsDuplicateEnvironmentKeys() {
        assertThrows(IllegalArgumentException.class, () -> new CreateProjectRequest(
                "analytics",
                "Analytics",
                List.of(
                        new EnvironmentSpec("DEV", "Development"),
                        new EnvironmentSpec("dev", "Development Copy")
                ),
                Map.of(),
                CONTEXT
        ));
    }
}
