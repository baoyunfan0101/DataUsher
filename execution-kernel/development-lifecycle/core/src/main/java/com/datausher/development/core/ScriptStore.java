package com.datausher.development.core;

import com.datausher.development.api.ScriptDefinition;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptVersion;

import java.util.List;
import java.util.Optional;

public interface ScriptStore {
    void createScript(ScriptDefinition script);

    void deleteScript(ScriptDefinition script);

    void createVersion(
            ScriptDefinition expectedScript,
            ScriptDefinition updatedScript,
            ScriptVersion version
    );

    void deleteVersion(
            ScriptDefinition expectedScript,
            ScriptDefinition restoredScript,
            ScriptVersion version
    );

    Optional<ScriptDefinition> findScript(ScriptId scriptId);

    Optional<ScriptVersion> findVersion(ScriptId scriptId, long version);

    List<ScriptVersion> listVersions(ScriptId scriptId);
}
