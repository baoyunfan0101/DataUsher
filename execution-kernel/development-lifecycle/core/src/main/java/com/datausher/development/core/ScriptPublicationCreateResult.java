package com.datausher.development.core;

import java.util.Objects;

public record ScriptPublicationCreateResult(StoredScriptPublication publication, boolean created) {
    public ScriptPublicationCreateResult {
        publication = Objects.requireNonNull(publication, "publication must not be null");
    }
}
