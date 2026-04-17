package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
        Long memoryId = currentUserId();
        String normalizedUserMessage = normalizeUserMessage(userMessage);
        return new AssistantBackedTokenStream(assistant, memoryId, normalizedUserMessage);
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

    private static final class AssistantBackedTokenStream implements TokenStream {

        private static final int STREAM_CHUNK_SIZE = 24;

        private final Assistant assistant;
        private final Long memoryId;
        private final String userMessage;
        private Consumer<String> onNext = ignored -> {
        };
        private Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved = ignored -> {
        };
        private Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted = ignored -> {
        };
        private Consumer<Response<AiMessage>> onComplete = ignored -> {
        };
        private Consumer<Throwable> onError = ignored -> {
        };
        private boolean ignoreErrors;

        private AssistantBackedTokenStream(Assistant assistant, Long memoryId, String userMessage) {
            this.assistant = assistant;
            this.memoryId = memoryId;
            this.userMessage = userMessage;
        }

        @Override
        public TokenStream onNext(Consumer<String> onNext) {
            this.onNext = onNext;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved) {
            this.onRetrieved = onRetrieved;
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted) {
            this.onToolExecuted = onToolExecuted;
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
            this.ignoreErrors = true;
            return this;
        }

        @Override
        public void start() {
            CompletableFuture.runAsync(() -> {
                try {
                    onRetrieved.accept(List.of());
                    String reply = assistant.chat(memoryId, userMessage);
                    emitChunks(reply == null ? "" : reply);
                    onComplete.accept(Response.from(AiMessage.aiMessage(reply == null ? "" : reply)));
                } catch (Throwable throwable) {
                    if (ignoreErrors) {
                        return;
                    }
                    onError.accept(throwable);
                }
            });
        }

        private void emitChunks(String reply) {
            if (reply.isEmpty()) {
                return;
            }
            for (int index = 0; index < reply.length(); index += STREAM_CHUNK_SIZE) {
                int endIndex = Math.min(index + STREAM_CHUNK_SIZE, reply.length());
                onNext.accept(reply.substring(index, endIndex));
            }
        }
    }
}
