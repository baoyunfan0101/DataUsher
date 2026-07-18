package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.CredentialBinding;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryCredentialBindingServiceTest {
    @Test
    void rotatesCredentialReferencesWithMonotonicRevisions() {
        InMemoryCredentialBindingService service = new InMemoryCredentialBindingService();
        CredentialBinding initial = binding(1, "vault://data/mysql-v1");
        CredentialBinding rotated = binding(2, "vault://data/mysql-v2");

        assertEquals(initial, service.bind(initial));
        assertEquals(initial, service.bind(initial));
        assertEquals(rotated, service.bind(rotated));

        assertEquals(rotated, service.find("ANALYTICS").orElseThrow());
        assertEquals(List.of(rotated), service.listByAdapter("mysql"));
        assertEquals(rotated, service.unbind("analytics").orElseThrow());
    }

    @Test
    void rejectsSkippedRevisionsAndAdapterReassignment() {
        InMemoryCredentialBindingService service = new InMemoryCredentialBindingService();
        service.bind(binding(1, "vault://data/mysql-v1"));

        assertThrows(IllegalStateException.class,
                () -> service.bind(binding(3, "vault://data/mysql-v3")));
        assertThrows(IllegalStateException.class, () -> service.bind(new CredentialBinding(
                "analytics", "postgres", URI.create("vault://data/postgres"), 2, Map.of())));
    }

    private static CredentialBinding binding(long revision, String reference) {
        return new CredentialBinding(
                "analytics", "mysql", URI.create(reference), revision, Map.of());
    }
}
