package com.datausher.development.api;

public interface ScriptCommandService {
    ScriptDefinition create(CreateScriptRequest request);

    ScriptVersion createVersion(CreateScriptVersionRequest request);
}
