package com.datausher.governance.ownership.api;

import java.util.Objects;

public record OwnershipRole(String value) {
    public static final OwnershipRole PRIMARY = new OwnershipRole("primary");
    public static final OwnershipRole BUSINESS = new OwnershipRole("business");
    public static final OwnershipRole TECHNICAL = new OwnershipRole("technical");

    public OwnershipRole {
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
