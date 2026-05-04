package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.BookingWorkflowService;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowService;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowService;
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
    private final BookingWorkflowService bookingWorkflowService;
    private final CancelWorkflowService cancelWorkflowService;
    private final RescheduleWorkflowService rescheduleWorkflowService;
    private final ChatMemoryStore chatMemoryStore;
    private final AiChatProfiler aiChatProfiler;

    public AiChatServiceImpl(
            Assistant assistant,
            BookingWorkflowService bookingWorkflowService,
            CancelWorkflowService cancelWorkflowService,
            RescheduleWorkflowService rescheduleWorkflowService,
            ChatMemoryStore chatMemoryStore,
            AiChatProfiler aiChatProfiler
    ) {
        this.assistant = assistant;
        this.bookingWorkflowService = bookingWorkflowService;
        this.cancelWorkflowService = cancelWorkflowService;
        this.rescheduleWorkflowService = rescheduleWorkflowService;
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
        if (bookingWorkflowService.hasActiveTask(userId)) {
            String bookingReply = bookingWorkflowService.handle(userId, normalizedUserMessage);
            if (isBookingWorkflowAborted(bookingReply)) {
                return assistant.chat(userId, normalizedUserMessage);
            }
            return bookingReply;
        }
        if (cancelWorkflowService.hasActiveTask(userId)
                || cancelWorkflowService.shouldStartWorkflow(userId, originalUserMessage)) {
            return cancelWorkflowService.handle(userId, normalizedUserMessage);
        }
        if (rescheduleWorkflowService.hasActiveTask(userId)
                || rescheduleWorkflowService.shouldStartWorkflow(userId, originalUserMessage)) {
            return rescheduleWorkflowService.handle(userId, normalizedUserMessage);
        }
        if (bookingWorkflowService.shouldStartWorkflow(userId, originalUserMessage)) {
            return bookingWorkflowService.handle(userId, normalizedUserMessage);
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
        if (bookingWorkflowService.hasActiveTask(memoryId)) {
            String bookingReply = bookingWorkflowService.handle(memoryId, normalizedUserMessage);
            if (isBookingWorkflowAborted(bookingReply)) {
                return assistant.streamChat(memoryId, normalizedUserMessage);
            }
            return new SingleReplyTokenStream(bookingReply);
        }
        if (cancelWorkflowService.hasActiveTask(memoryId)
                || cancelWorkflowService.shouldStartWorkflow(memoryId, originalUserMessage)) {
            return cancelWorkflowService.streamHandle(memoryId, normalizedUserMessage);
        }
        if (rescheduleWorkflowService.hasActiveTask(memoryId)
                || rescheduleWorkflowService.shouldStartWorkflow(memoryId, originalUserMessage)) {
            return rescheduleWorkflowService.streamHandle(memoryId, normalizedUserMessage);
        }
        if (bookingWorkflowService.shouldStartWorkflow(memoryId, originalUserMessage)) {
            return bookingWorkflowService.streamHandle(memoryId, normalizedUserMessage);
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

    private boolean isBookingWorkflowAborted(String workflowReply) {
        return workflowReply != null
                && workflowReply.startsWith(BookingWorkflowServiceImpl.BOOKING_TASK_ABORTED_MARKER);
    }

    private static class SingleReplyTokenStream implements TokenStream {

        private final String reply;
        private java.util.function.Consumer<String> onNext = ignored -> { };
        private java.util.function.Consumer<Response<AiMessage>> onComplete = ignored -> { };
        private java.util.function.Consumer<Throwable> onError = ignored -> { };

        private SingleReplyTokenStream(String reply) {
            this.reply = reply;
        }

        @Override
        public TokenStream onRetrieved(java.util.function.Consumer<java.util.List<dev.langchain4j.rag.content.Content>> consumer) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution> consumer) {
            return this;
        }

        @Override
        public TokenStream onComplete(java.util.function.Consumer<Response<AiMessage>> consumer) {
            this.onComplete = consumer;
            return this;
        }

        @Override
        public TokenStream onError(java.util.function.Consumer<Throwable> consumer) {
            this.onError = consumer;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            try {
                if (reply != null && !reply.isBlank()) {
                    onNext.accept(reply);
                }
                onComplete.accept(Response.from(AiMessage.from(reply == null ? "" : reply)));
            } catch (Throwable throwable) {
                onError.accept(throwable);
            }
        }

        @Override
        public TokenStream onNext(java.util.function.Consumer<String> consumer) {
            this.onNext = consumer;
            return this;
        }
    }
}
