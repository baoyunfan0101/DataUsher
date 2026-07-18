package com.datausher.governance.approval.api;

import java.util.Objects;

public record ApprovalRequestId(String value) {
    public ApprovalRequestId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
