package com.datausher.ai.runtime.api;

import java.util.List;
import java.util.Optional;

public interface AiConversationService {
    AiConversation start(StartAiConversationRequest request);

    AiMessage appendMessage(AppendAiMessageRequest request);

    Optional<AiConversation> findConversation(AiConversationId conversationId);

    List<AiMessage> listMessages(AiConversationId conversationId);

    AiProviderCallResult callProvider(AiProviderCallRequest request);
}
