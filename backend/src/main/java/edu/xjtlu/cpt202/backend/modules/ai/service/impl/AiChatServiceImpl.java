package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowService;
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
    private final CancelWorkflowService cancelWorkflowService;
    private final ChatMemoryStore chatMemoryStore;
    private final AiChatProfiler aiChatProfiler;

    public AiChatServiceImpl(
            Assistant assistant,
            CancelWorkflowService cancelWorkflowService,
            ChatMemoryStore chatMemoryStore,
            AiChatProfiler aiChatProfiler
    ) {
        this.assistant = assistant;
        this.cancelWorkflowService = cancelWorkflowService;
        this.chatMemoryStore = chatMemoryStore;
        this.aiChatProfiler = aiChatProfiler;
    }

    @Override
    public String chat(String userMessage) {
        long startNs = System.nanoTime();
        Long userId = currentUserId();
        String normalizedUserMessage = normalizeUserMessage(userMessage);
        aiChatProfiler.logStage("service.chat.normalize", elapsedMs(startNs), java.util.Map.of(
                "userId", userId,
                "messageLength", normalizedUserMessage.length()
        ));
        String originalUserMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
        if (cancelWorkflowService.hasActiveTask(userId)
                || cancelWorkflowService.shouldStartWorkflow(userId, originalUserMessage)) {
            return cancelWorkflowService.handle(userId, normalizedUserMessage);
        }
        return assistant.chat(userId, normalizedUserMessage);
    }

    @Override
    public TokenStream streamChat(String userMessage) {
        long startNs = System.nanoTime();
        Long memoryId = currentUserId();
        String normalizedUserMessage = normalizeUserMessage(userMessage);
        aiChatProfiler.logStage("service.streamChat.normalize", elapsedMs(startNs), java.util.Map.of(
                "memoryId", memoryId,
                "messageLength", normalizedUserMessage.length()
        ));
        String originalUserMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
        if (cancelWorkflowService.hasActiveTask(memoryId)
                || cancelWorkflowService.shouldStartWorkflow(memoryId, originalUserMessage)) {
            return cancelWorkflowService.streamHandle(memoryId, normalizedUserMessage);
        }
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

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

}
