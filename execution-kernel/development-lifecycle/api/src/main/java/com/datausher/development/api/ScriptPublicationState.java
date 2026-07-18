package com.datausher.development.api;

public enum ScriptPublicationState {
    REQUESTED,
    APPROVAL_PENDING,
    PUBLISHED,
    REJECTED,
    CANCELLED,
    CONFLICTED;

    public boolean terminal() {
        return this == PUBLISHED || this == REJECTED || this == CANCELLED || this == CONFLICTED;
    }
}
