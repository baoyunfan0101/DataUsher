package com.datausher.ai.runtime.core;

import com.datausher.ai.runtime.api.AiConversation;
import com.datausher.ai.runtime.api.AiConversationId;
import com.datausher.ai.runtime.api.AiMessage;
import com.datausher.ai.runtime.api.AiToolInvocation;
import com.datausher.ai.runtime.api.AiToolInvocationId;

import java.util.List;
import java.util.Optional;

public interface AiRuntimeStore {
    AiConversation createConversation(AiConversation conversation);

    AiMessage appendMessage(AiMessage message);

    Optional<AiConversation> findConversation(AiConversationId conversationId);

    List<AiMessage> listMessages(AiConversationId conversationId);

    AiToolInvocation createOrFindInvocation(AiToolInvocation invocation);

    AiToolInvocation updateInvocation(AiToolInvocation expected, AiToolInvocation replacement);

    Optional<AiToolInvocation> findInvocation(AiToolInvocationId invocationId);
}
