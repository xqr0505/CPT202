package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private final Assistant assistant;
    private final ChatMemoryStore chatMemoryStore;

    public AiChatServiceImpl(Assistant assistant, ChatMemoryStore chatMemoryStore) {
        this.assistant = assistant;
        this.chatMemoryStore = chatMemoryStore;
    }

    @Override
    public String chat(String userMessage) {
        return assistant.chat(currentUserId(), normalizeUserMessage(userMessage));
    }

    @Override
    public TokenStream streamChat(String userMessage) {
        return assistant.streamChat(currentUserId(), normalizeUserMessage(userMessage));
    }

    @Override
    public void clearCurrentUserMemory() {
        chatMemoryStore.deleteMessages(currentUserId());
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String normalizeUserMessage(String userMessage) {
        return Optional.ofNullable(userMessage).orElse("");
    }
}
