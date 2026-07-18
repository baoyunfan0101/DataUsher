package com.datausher.development.api;

import java.util.List;
import java.util.Optional;

public interface ScriptQueryService {
    Optional<ScriptDefinition> findScript(ScriptId scriptId);

    Optional<ScriptVersion> findVersion(ScriptId scriptId, long version);

    Optional<ScriptVersion> findLatestVersion(ScriptId scriptId);

    List<ScriptVersion> listVersions(ScriptId scriptId);
}
