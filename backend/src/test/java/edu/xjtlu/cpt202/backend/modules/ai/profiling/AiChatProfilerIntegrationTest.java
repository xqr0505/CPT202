package edu.xjtlu.cpt202.backend.modules.ai.profiling;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowService;
import edu.xjtlu.cpt202.backend.modules.ai.service.impl.AiChatServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatProfilerIntegrationTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AiChatProfiler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (logger != null && listAppender != null) {
            logger.detachAppender(listAppender);
        }
        UserContextHolder.clear();
    }

    @Test
    void shouldLogAiChatProfilingStages() {
        CommonProperties commonProperties = new CommonProperties();
        commonProperties.getLogging().setAiChatProfilingEnabled(true);
        AiChatProfiler profiler = new AiChatProfiler(commonProperties);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(
                new StubAssistant(profiler),
                new NoOpCancelWorkflowService(),
                new NoopChatMemoryStore(),
                profiler
        );

        UserContextHolder.setUserId(1001L);
        profiler.startTrace("test.start", 1001L, "hello");
        TokenStream tokenStream = aiChatService.streamChat("Hello");
        tokenStream.onNext(token -> { })
                .onComplete(response -> { })
                .onError(error -> { })
                .start();

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("stage=service.streamChat.normalize")));
        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("stage=assistant.stream.timeToFirstToken")));
        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("stage=assistant.stream.completed")));
    }

    private static class StubAssistant implements Assistant {

        private final AiChatProfiler profiler;

        private StubAssistant(AiChatProfiler profiler) {
            this.profiler = profiler;
        }

        @Override
        public String chat(Long memoryId, String userMessage) {
            return userMessage;
        }

        @Override
        public TokenStream streamChat(Long memoryId, String userMessage) {
            return new TokenStream() {
                private Consumer<String> onNext = ignored -> { };
                private Consumer<Response<AiMessage>> onComplete = ignored -> { };
                private Consumer<Throwable> onError = ignored -> { };

                @Override
                public TokenStream onNext(Consumer<String> onNext) {
                    this.onNext = onNext;
                    return this;
                }

                @Override
                public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved) {
                    return this;
                }

                @Override
                public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted) {
                    return this;
                }

                @Override
                public TokenStream onComplete(Consumer<Response<AiMessage>> onComplete) {
                    this.onComplete = onComplete;
                    return this;
                }

                @Override
                public TokenStream onError(Consumer<Throwable> onError) {
                    this.onError = onError;
                    return this;
                }

                @Override
                public TokenStream ignoreErrors() {
                    return this;
                }

                @Override
                public void start() {
                    try {
                        profiler.logStage("assistant.stream.timeToFirstToken", 5L, java.util.Map.of(
                                "memoryId", memoryId
                        ));
                        onNext.accept("hi");
                        profiler.logSummary("assistant.stream.completed", 10L, java.util.Map.of(
                                "memoryId", memoryId
                        ));
                        onComplete.accept(Response.from(AiMessage.from("hi")));
                    } catch (RuntimeException exception) {
                        onError.accept(exception);
                    }
                }
            };
        }
    }

    private static class NoopChatMemoryStore implements dev.langchain4j.store.memory.chat.ChatMemoryStore {

        @Override
        public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
            return List.of();
        }

        @Override
        public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        }

        @Override
        public void deleteMessages(Object memoryId) {
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
            throw new UnsupportedOperationException();
        }
    }
}
