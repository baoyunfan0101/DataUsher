package com.datausher.ai.runtime.api;

import java.util.Optional;

public interface AiToolInvocationService {
    AiToolInvocation start(StartAiToolInvocationRequest request);

    AiToolInvocation complete(CompleteAiToolInvocationRequest request);

    Optional<AiToolInvocation> find(AiToolInvocationId invocationId);
}
