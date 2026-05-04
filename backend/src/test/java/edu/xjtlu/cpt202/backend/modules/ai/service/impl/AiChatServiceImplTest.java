package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowService;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
                new NoOpCancelWorkflowService(),
                new NoOpRescheduleWorkflowService(),
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
                new NoOpCancelWorkflowService(),
                new NoOpRescheduleWorkflowService(),
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

    @Test
    void shouldShortCircuitToCancelWorkflowWhenTaskIsActive() {
        EchoAssistant assistant = new EchoAssistant();
        RecordingCancelWorkflowService cancelWorkflowService = new RecordingCancelWorkflowService(true, false);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                assistant,
                cancelWorkflowService,
                new NoOpRescheduleWorkflowService(),
                new InMemoryChatMemoryStore(),
                aiChatProfiler
        );

        UserContextHolder.setUserId(1001L);
        String reply = aiChatService.chat("Cancel booking 15");

        assertEquals("cancel-workflow", reply);
        assertEquals("Cancel booking 15", cancelWorkflowService.lastOriginalMessage());
    }

    @Test
    void shouldStartCancelWorkflowOnFirstCancelIntent() {
        EchoAssistant assistant = new EchoAssistant();
        RecordingCancelWorkflowService cancelWorkflowService = new RecordingCancelWorkflowService(false, true);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                assistant,
                cancelWorkflowService,
                new NoOpRescheduleWorkflowService(),
                new InMemoryChatMemoryStore(),
                aiChatProfiler
        );

        UserContextHolder.setUserId(1001L);
        String reply = aiChatService.chat("Please cancel my booking");

        assertEquals("cancel-workflow", reply);
        assertEquals("Please cancel my booking", cancelWorkflowService.lastOriginalMessage());
    }

    @Test
    void shouldUseNormalAssistantWhenCancelWorkflowIsNotActive() {
        EchoAssistant assistant = new EchoAssistant();
        RecordingCancelWorkflowService cancelWorkflowService = new RecordingCancelWorkflowService(false, false);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                assistant,
                cancelWorkflowService,
                new NoOpRescheduleWorkflowService(),
                new InMemoryChatMemoryStore(),
                aiChatProfiler
        );

        UserContextHolder.setUserId(1001L);
        String reply = aiChatService.chat("Check upcoming bookings");

        assertTrue(reply.contains("Check upcoming bookings"));
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

    private static class NoOpCancelWorkflowService implements CancelWorkflowService {

        @Override
        public boolean hasActiveTask(Long userId) {
            return false;
        }

        @Override
        public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
            return false;
        }

        @Override
        public String handle(Long userId, String normalizedUserMessage) {
            return normalizedUserMessage;
        }

        @Override
        public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
            return new SimpleTokenStream(normalizedUserMessage);
        }
    }

    private static class NoOpRescheduleWorkflowService implements RescheduleWorkflowService {

        @Override
        public boolean hasActiveTask(Long userId) {
            return false;
        }

        @Override
        public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
            return false;
        }

        @Override
        public String handle(Long userId, String normalizedUserMessage) {
            return normalizedUserMessage;
        }

        @Override
        public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
            return new SimpleTokenStream(normalizedUserMessage);
        }
    }

    private static class RecordingCancelWorkflowService implements CancelWorkflowService {

        private final boolean activeTask;
        private final boolean shouldStart;
        private String lastOriginalMessage;

        private RecordingCancelWorkflowService(boolean activeTask, boolean shouldStart) {
            this.activeTask = activeTask;
            this.shouldStart = shouldStart;
        }

        @Override
        public boolean hasActiveTask(Long userId) {
            return activeTask;
        }

        @Override
        public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
            this.lastOriginalMessage = originalUserMessage;
            return shouldStart;
        }

        @Override
        public String handle(Long userId, String normalizedUserMessage) {
            this.lastOriginalMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
            return "cancel-workflow";
        }

        @Override
        public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
            this.lastOriginalMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
            return new SimpleTokenStream("cancel-workflow");
        }

        private String lastOriginalMessage() {
            return lastOriginalMessage;
        }
    }

    private static class SimpleTokenStream implements TokenStream {

        private final String reply;
        private java.util.function.Consumer<String> onNext = ignored -> { };
        private java.util.function.Consumer<Response<AiMessage>> onComplete = ignored -> { };
        private java.util.function.Consumer<Throwable> onError = ignored -> { };
        private final AtomicBoolean ignoreErrors = new AtomicBoolean(false);

        private SimpleTokenStream(String reply) {
            this.reply = reply;
        }

        @Override
        public TokenStream onNext(java.util.function.Consumer<String> onNext) {
            this.onNext = onNext;
            return this;
        }

        @Override
        public TokenStream onRetrieved(java.util.function.Consumer<java.util.List<dev.langchain4j.rag.content.Content>> onRetrieved) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted) {
            return this;
        }

        @Override
        public TokenStream onComplete(java.util.function.Consumer<Response<AiMessage>> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        @Override
        public TokenStream onError(java.util.function.Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            ignoreErrors.set(true);
            return this;
        }

        @Override
        public void start() {
            try {
                onNext.accept(reply);
                onComplete.accept(Response.from(AiMessage.from(reply)));
            } catch (Throwable throwable) {
                if (!ignoreErrors.get()) {
                    onError.accept(throwable);
                }
            }
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
