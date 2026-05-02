package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author QiranXiao
 * @since 2026/4/15
 *
 */
class AiChatServiceImplTest {

    private final AiChatProfiler aiChatProfiler = new AiChatProfiler(new CommonProperties());

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldKeepContextPerCurrentUserAndClearMemory() {
        InMemoryChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                new MemoryAwareAssistant(chatMemoryStore),
                chatMemoryStore,
                aiChatProfiler
        );

        UserContextHolder.setUserId(1001L);
        String firstReply = aiChatService.chat("Hello");
        String secondReply = aiChatService.chat("Can you remember me?");

        UserContextHolder.setUserId(2002L);
        String otherUserReply = aiChatService.chat("Hello from another user");

        UserContextHolder.setUserId(1001L);
        aiChatService.clearCurrentUserMemory();
        String replyAfterClear = aiChatService.chat("Start over");

        assertEquals("history=0", firstReply);
        assertEquals("history=2", secondReply);
        assertEquals("history=0", otherUserReply);
        assertEquals("history=0", replyAfterClear);
    }

    @Test
    void shouldIncludeCurrentSystemTimeInPrompt() {
        EchoAssistant echoAssistant = new EchoAssistant();
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                echoAssistant,
                new InMemoryChatMemoryStore(),
                aiChatProfiler
        );

        UserContextHolder.setUserId(1001L);
        String prompt = aiChatService.chat("Check upcoming bookings");

        assertTrue(prompt.contains("Current system time:"));
        assertTrue(prompt.contains("Use this as the authoritative current time"));
        assertTrue(prompt.contains("User message:"));
        assertTrue(prompt.contains("Check upcoming bookings"));
    }

    private static class MemoryAwareAssistant implements Assistant {

        private final ChatMemoryStore chatMemoryStore;

        private MemoryAwareAssistant(ChatMemoryStore chatMemoryStore) {
            this.chatMemoryStore = chatMemoryStore;
        }

        @Override
        public String chat(Long memoryId, String userMessage) {
            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20)
                    .chatMemoryStore(chatMemoryStore)
                    .build();

            int historySize = memory.messages().size();
            memory.add(UserMessage.userMessage(userMessage));
            memory.add(AiMessage.aiMessage("ok"));
            return "history=" + historySize;
        }

        @Override
        public TokenStream streamChat(Long memoryId, String userMessage) {
            throw new UnsupportedOperationException("Not needed in this test");
        }
    }

    private static class EchoAssistant implements Assistant {

        @Override
        public String chat(Long memoryId, String userMessage) {
            return userMessage;
        }

        @Override
        public TokenStream streamChat(Long memoryId, String userMessage) {
            throw new UnsupportedOperationException("Not needed in this test");
        }
    }

    private static class InMemoryChatMemoryStore implements ChatMemoryStore {

        private final Map<Object, List<dev.langchain4j.data.message.ChatMessage>> memory = new HashMap<>();

        @Override
        public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
            return memory.getOrDefault(memoryId, List.of());
        }

        @Override
        public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
            memory.put(memoryId, List.copyOf(messages));
        }

        @Override
        public void deleteMessages(Object memoryId) {
            memory.remove(memoryId);
        }
    }
}
