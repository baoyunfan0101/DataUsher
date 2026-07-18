package com.datausher.integration.datasource.contract;

import com.datausher.integration.datasource.api.ConnectionTestResult;
import com.datausher.integration.datasource.api.DatasourceCapabilities;
import com.datausher.integration.datasource.api.DatasourceConnection;
import com.datausher.integration.datasource.api.DatasourceConnector;
import com.datausher.integration.datasource.api.DatasourceObject;
import com.datausher.integration.datasource.api.DatasourceObjectAttributes;
import com.datausher.integration.datasource.api.DatasourceObjectKinds;
import com.datausher.integration.datasource.api.DiscoveryRequest;
import com.datausher.integration.runtime.api.AdapterDescriptor;
import com.datausher.integration.runtime.api.AdapterHealth;
import com.datausher.integration.runtime.api.AdapterRequestContext;
import com.datausher.integration.runtime.api.AdapterType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DatasourceConnectorContract {
    private static final Set<String> STANDARD_KINDS = Set.of(
            DatasourceObjectKinds.DATABASE,
            DatasourceObjectKinds.TABLE,
            DatasourceObjectKinds.COLUMN,
            DatasourceObjectKinds.PARTITION
    );

    protected abstract DatasourceConnector connector();

    protected abstract DatasourceConnection validConnection();

    protected AdapterRequestContext requestContext() {
        return new AdapterRequestContext(
                "contract-request",
                Instant.parse("2099-01-01T00:00:00Z"),
                Map.of("test", "contract")
        );
    }

    protected DiscoveryRequest discoveryRequest() {
        return new DiscoveryRequest(validConnection(), "", Map.of());
    }

    @Test
    void declaresCanonicalDatasourceIdentityAndDiscoveryCapability() {
        AdapterDescriptor descriptor = connector().descriptor();

        assertEquals(AdapterType.DATASOURCE, descriptor.type());
        assertTrue(descriptor.supports(DatasourceCapabilities.DISCOVERY));
        assertFalse(descriptor.version().isBlank());
    }

    @Test
    void reportsHealthForTheDeclaredAdapterIdentity() {
        AdapterDescriptor descriptor = connector().descriptor();
        AdapterHealth health = connector().checkHealth();

        assertNotNull(health);
        assertEquals(descriptor.adapterId(), health.adapterId());
    }

    @Test
    void acceptsAValidConnectionFixture() {
        ConnectionTestResult result = connector().testConnection(
                requestContext(), validConnection());

        assertNotNull(result);
        assertTrue(result.successful(), result.message());
    }

    @Test
    void discoversCanonicalUniqueObjectsWithoutCredentialAttributes() {
        List<DatasourceObject> objects = connector().discover(
                requestContext(), discoveryRequest());
        HashSet<String> externalIds = new HashSet<>();

        for (DatasourceObject object : objects) {
            assertTrue(STANDARD_KINDS.contains(object.kind()),
                    () -> "non-standard datasource object kind: " + object.kind());
            String externalId = object.attributes().get(
                    DatasourceObjectAttributes.EXTERNAL_ID);
            assertNotNull(externalId, "externalId attribute is required");
            assertTrue(externalIds.add(externalId),
                    () -> "duplicate externalId: " + externalId);
            assertTrue(object.attributes().keySet().stream().noneMatch(
                    key -> key.toLowerCase(java.util.Locale.ROOT)
                            .matches(".*(password|passwd|secret|token|credential).*$")));
            if (!object.kind().equals(DatasourceObjectKinds.DATABASE)) {
                assertFalse(object.attributes().getOrDefault(
                        DatasourceObjectAttributes.PARENT_EXTERNAL_ID, "").isBlank());
            }
        }
    }
}
