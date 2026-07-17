package com.datausher.integration.runtime.core;

import com.datausher.integration.runtime.api.CredentialBinding;
import com.datausher.integration.runtime.api.CredentialBindingService;
import com.datausher.integration.runtime.api.IntegrationIdentifiers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCredentialBindingService implements CredentialBindingService {
    private final ConcurrentMap<String, CredentialBinding> bindings = new ConcurrentHashMap<>();

    @Override
    public CredentialBinding bind(CredentialBinding binding) {
        Objects.requireNonNull(binding, "binding must not be null");
        bindings.compute(binding.bindingId(), (bindingId, existing) -> {
            if (existing == null) {
                if (binding.revision() != 1) {
                    throw new IllegalStateException(
                            "initial credential binding revision must be 1: " + bindingId);
                }
                return binding;
            }
            if (existing.equals(binding)) {
                return existing;
            }
            if (!existing.adapterId().equals(binding.adapterId())) {
                throw new IllegalStateException(
                        "credential binding adapter cannot change: " + bindingId);
            }
            if (binding.revision() != existing.revision() + 1) {
                throw new IllegalStateException(
                        "credential binding revision must advance by one: " + bindingId);
            }
            return binding;
        });
        return binding;
    }

    @Override
    public Optional<CredentialBinding> unbind(String bindingId) {
        return Optional.ofNullable(bindings.remove(normalize(bindingId, "bindingId")));
    }

    @Override
    public Optional<CredentialBinding> find(String bindingId) {
        return Optional.ofNullable(bindings.get(normalize(bindingId, "bindingId")));
    }

    @Override
    public List<CredentialBinding> listByAdapter(String adapterId) {
        String normalized = normalize(adapterId, "adapterId");
        return bindings.values().stream()
                .filter(binding -> binding.adapterId().equals(normalized))
                .sorted(Comparator.comparing(CredentialBinding::bindingId))
                .toList();
    }

    private static String normalize(String value, String fieldName) {
        return IntegrationIdentifiers.normalize(value, fieldName);
    }
}
