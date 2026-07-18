package com.datausher.execution.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ExecutionQuery(
        Set<ExecutionState> states,
        Optional<ExecutionWorkloadType> workloadType,
        Optional<ExecutionQueueId> queueId,
        Optional<ExecutionAccountId> accountId,
        Optional<Instant> submittedFrom,
        Optional<Instant> submittedUntil
) {
    public ExecutionQuery {
        states = states == null ? Set.of() : Set.copyOf(states);
        workloadType = optional(workloadType);
        queueId = optional(queueId);
        accountId = optional(accountId);
        submittedFrom = optional(submittedFrom);
        submittedUntil = optional(submittedUntil);
        if (submittedFrom.isPresent() && submittedUntil.isPresent()
                && submittedUntil.get().isBefore(submittedFrom.get())) {
            throw new IllegalArgumentException("submittedUntil must not precede submittedFrom");
        }
    }

    public static ExecutionQuery all() {
        return new ExecutionQuery(Set.of(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return Objects.requireNonNullElse(value, Optional.empty());
    }
}
