package com.datausher.development.api;

import java.util.Optional;

public interface ScriptPublicationService {
    ScriptPublication request(RequestScriptPublication request);

    Optional<ScriptPublication> findPublication(ScriptPublicationId publicationId);
}
