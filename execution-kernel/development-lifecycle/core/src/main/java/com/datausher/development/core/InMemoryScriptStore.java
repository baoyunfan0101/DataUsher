package com.datausher.development.core;

import com.datausher.development.api.ScriptDefinition;
import com.datausher.development.api.ScriptId;
import com.datausher.development.api.ScriptVersion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryScriptStore implements ScriptStore {
    private final Map<ScriptId, ScriptDefinition> scripts = new HashMap<>();
    private final Map<String, ScriptVersion> versions = new HashMap<>();

    @Override
    public synchronized void createScript(ScriptDefinition script) {
        if (scripts.putIfAbsent(script.scriptId(), script) != null) {
            throw new IllegalStateException("script already exists: " + script.scriptId());
        }
    }

    @Override
    public synchronized void deleteScript(ScriptDefinition script) {
        if (!scripts.remove(script.scriptId(), script)) {
            throw new IllegalStateException("script changed before rollback: " + script.scriptId());
        }
    }

    @Override
    public synchronized void createVersion(
            ScriptDefinition expectedScript,
            ScriptDefinition updatedScript,
            ScriptVersion version
    ) {
        if (!scripts.replace(expectedScript.scriptId(), expectedScript, updatedScript)) {
            throw new IllegalStateException("script changed concurrently: " + expectedScript.scriptId());
        }
        String key = versionKey(version.scriptId(), version.version());
        if (versions.putIfAbsent(key, version) != null) {
            scripts.put(expectedScript.scriptId(), expectedScript);
            throw new IllegalStateException("script version already exists: " + key);
        }
    }

    @Override
    public synchronized void deleteVersion(
            ScriptDefinition expectedScript,
            ScriptDefinition restoredScript,
            ScriptVersion version
    ) {
        if (!scripts.replace(expectedScript.scriptId(), expectedScript, restoredScript)
                || !versions.remove(versionKey(version.scriptId(), version.version()), version)) {
            throw new IllegalStateException("script version changed before rollback");
        }
    }

    @Override
    public synchronized Optional<ScriptDefinition> findScript(ScriptId scriptId) {
        return Optional.ofNullable(scripts.get(scriptId));
    }

    @Override
    public synchronized Optional<ScriptVersion> findVersion(ScriptId scriptId, long version) {
        return Optional.ofNullable(versions.get(versionKey(scriptId, version)));
    }

    @Override
    public synchronized List<ScriptVersion> listVersions(ScriptId scriptId) {
        return versions.values().stream()
                .filter(version -> version.scriptId().equals(scriptId))
                .sorted(Comparator.comparingLong(ScriptVersion::version))
                .toList();
    }

    private static String versionKey(ScriptId scriptId, long version) {
        return scriptId.value().length() + ":" + scriptId.value() + ":" + version;
    }
}
