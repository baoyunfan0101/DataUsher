package com.datausher.development.core;

import com.datausher.development.api.ScriptPublication;

import java.util.Objects;

public record ScriptPublicationTransitionResult(ScriptPublication publication, boolean changed) {
    public ScriptPublicationTransitionResult {
        publication = Objects.requireNonNull(publication, "publication must not be null");
    }
}
