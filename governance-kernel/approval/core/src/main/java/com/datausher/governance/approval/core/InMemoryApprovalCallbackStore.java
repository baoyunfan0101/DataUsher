package com.datausher.governance.approval.core;

import com.datausher.governance.approval.api.ApprovalRequestId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryApprovalCallbackStore implements ApprovalCallbackStore {
    private final ConcurrentMap<String, StoredApprovalCallback> deliveries = new ConcurrentHashMap<>();

    @Override
    public Optional<StoredApprovalCallback> find(ApprovalRequestId approvalRequestId) {
        return Optional.ofNullable(deliveries.get(approvalRequestId.value()));
    }

    @Override
    public synchronized void replace(
            Optional<StoredApprovalCallback> expected,
            StoredApprovalCallback replacement
    ) {
        Objects.requireNonNull(expected, "expected must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");
        String key = replacement.delivery().approvalRequestId().value();
        StoredApprovalCallback current = deliveries.get(key);
        if (!Objects.equals(current, expected.orElse(null))) {
            throw new IllegalStateException("callback delivery changed concurrently: " + key);
        }
        deliveries.put(key, replacement);
    }
}
