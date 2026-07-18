package com.datausher.governance.approval.api;

import java.util.Objects;

public record ApproverSelectorType(String value) {
    public static final ApproverSelectorType SUBJECT = new ApproverSelectorType("subject");
    public static final ApproverSelectorType RESOURCE_OWNER = new ApproverSelectorType("resource-owner");

    public ApproverSelectorType {
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
