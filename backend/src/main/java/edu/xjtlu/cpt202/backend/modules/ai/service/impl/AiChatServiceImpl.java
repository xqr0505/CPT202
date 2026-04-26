package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter SYSTEM_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

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
        Long memoryId = currentUserId();
        String normalizedUserMessage = normalizeUserMessage(userMessage);
        return assistant.streamChat(memoryId, normalizedUserMessage);
    }

    @Override
    public void clearCurrentUserMemory() {
        chatMemoryStore.deleteMessages(currentUserId());
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String normalizeUserMessage(String userMessage) {
        String normalizedMessage = Optional.ofNullable(userMessage).orElse("");
        String currentSystemTime = ZonedDateTime.now().format(SYSTEM_TIME_FORMATTER);
        return """
                Current system time: %s
                Use this as the authoritative current time when interpreting relative dates such as today, tomorrow, upcoming, this week, and history.

                User message:
                %s
                """.formatted(currentSystemTime, normalizedMessage);
    }

}
