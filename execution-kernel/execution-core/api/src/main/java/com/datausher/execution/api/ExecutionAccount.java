package com.datausher.execution.api;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ExecutionAccount(
        ExecutionAccountId accountId,
        String displayName,
        String adapterId,
        String credentialBindingId,
        Set<ExecutionWorkloadType> workloadTypes,
        ExecutionAccountStatus status,
        Map<String, String> attributes,
        Instant createdAt,
        Instant updatedAt,
        long revision
) {
    public ExecutionAccount {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null").trim();
        adapterId = normalize(adapterId, "adapterId");
        credentialBindingId = normalize(credentialBindingId, "credentialBindingId");
        workloadTypes = workloadTypes == null ? Set.of() : Set.copyOf(workloadTypes);
        status = Objects.requireNonNull(status, "status must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be greater than zero");
        }
    }

    public boolean supports(ExecutionWorkloadType workloadType) {
        Objects.requireNonNull(workloadType, "workloadType must not be null");
        return workloadTypes.isEmpty() || workloadTypes.contains(workloadType);
    }

    public ExecutionAccount withStatus(ExecutionAccountStatus nextStatus, Instant changedAt) {
        return new ExecutionAccount(accountId, displayName, adapterId, credentialBindingId,
                workloadTypes, nextStatus, attributes, createdAt, changedAt, revision + 1);
    }

    private static String normalize(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9._:-]{0,126}")) {
            throw new IllegalArgumentException(
                    name + " must match [a-z0-9][a-z0-9._:-]{0,126}");
        }
        return normalized;
    }
}
