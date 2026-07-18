package com.datausher.governance.access.api;

import java.util.Objects;

public record SubjectType(String value) {
    public static final SubjectType USER = new SubjectType("user");
    public static final SubjectType GROUP = new SubjectType("group");
    public static final SubjectType SERVICE_ACCOUNT = new SubjectType("service-account");

    public SubjectType {
        value = Objects.requireNonNull(value, "value must not be null").trim().toLowerCase();
        if (!value.matches("[a-z][a-z0-9.-]{0,126}")) {
            throw new IllegalArgumentException("value must match [a-z][a-z0-9.-]{0,126}");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
