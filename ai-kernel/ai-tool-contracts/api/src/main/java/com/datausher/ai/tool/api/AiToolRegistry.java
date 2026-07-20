package com.datausher.ai.tool.api;

public interface AiToolRegistry {
    AiToolDefinition register(RegisterAiToolRequest request);

    AiToolDefinition changeStatus(ChangeAiToolStatusRequest request);
}
