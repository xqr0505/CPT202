package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiChatMemoryProperties;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiToolParallelProperties;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatTraceContext;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.util.ToolArgumentSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Assistant implementation that executes read-only tool calls in parallel.
 *
 * @author QiranXiao
 * @since 2026/4/23
 */
public class ParallelToolAssistant implements Assistant {

    private static final Logger log = LoggerFactory.getLogger(ParallelToolAssistant.class);
    private static final int MAX_TOOL_ROUNDS = 8;
    private static final int STREAM_CHUNK_SIZE = 24;
    private static final String BOOKING_SUBMIT_TOOL_NAME = "submitCurrentCustomerBooking";
    private static final String BOOKING_PREVIEW_MARKER = "AI_BOOKING_PREVIEW:";
    private static final ObjectMapper TOOL_ARGUMENT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ChatMemoryStore chatMemoryStore;
    private final AiChatMemoryProperties chatMemoryProperties;
    private final AiToolParallelProperties parallelProperties;
    private final AiChatProfiler aiChatProfiler;
    private final String systemPrompt;
    private final AiIntentRouterService intentRouterService;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;
    private final Map<AiIntent, ToolBox> intentToolBoxes;
    private final ExecutorService parallelExecutor;

    public ParallelToolAssistant(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            ChatMemoryStore chatMemoryStore,
            AiChatMemoryProperties chatMemoryProperties,
            AiToolParallelProperties parallelProperties,
            AiChatProfiler aiChatProfiler,
            String systemPrompt,
            List<Object> toolSources,
            AiIntentRouterService intentRouterService,
            Map<AiIntent, List<Object>> toolGroups
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.chatMemoryStore = chatMemoryStore;
        this.chatMemoryProperties = chatMemoryProperties;
        this.parallelProperties = parallelProperties;
        this.aiChatProfiler = aiChatProfiler;
        this.systemPrompt = systemPrompt;
        this.intentRouterService = intentRouterService;
        this.toolSpecifications = collectSpecifications(toolSources);
        this.toolExecutors = collectExecutors(toolSources);
        this.intentToolBoxes = collectIntentToolBoxes(toolGroups);
        this.parallelExecutor = Executors.newFixedThreadPool(
                parallelProperties.getMaxConcurrency(),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("ai-tool-parallel-" + THREAD_COUNTER.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    @Override
    public String chat(Long memoryId, String userMessage) {
        Response<AiMessage> response = runConversation(memoryId, userMessage, ignored -> {
        });
        AiMessage content = response.content();
        if (content == null || content.text() == null) {
            return AiConstant.EMPTY_CONTENT;
        }
        return content.text();
    }

    @Override
    public TokenStream streamChat(Long memoryId, String userMessage) {
        return new ParallelAssistantTokenStream(memoryId, userMessage);
    }

    private Response<AiMessage> runConversation(
            Long memoryId,
            String userMessage,
            Consumer<ToolExecution> onToolExecuted
    ) {
        long totalStartNs = System.nanoTime();
        List<ChatMessage> messages = new ArrayList<>(chatMemoryStore.getMessages(memoryId));
        ensureSystemMessage(messages);
        addMessageToWindow(messages, UserMessage.userMessage(userMessage));

        long firstInvokeStartNs = System.nanoTime();
        Response<AiMessage> response = invokeModel(messages);
        aiChatProfiler.logStage("assistant.invokeModel.first", elapsedMs(firstInvokeStartNs), Map.of(
                "memoryId", memoryId,
                "messageCount", messages.size()
        ));
        AiMessage aiMessage = response.content();
        if (aiMessage == null) {
            chatMemoryStore.updateMessages(memoryId, messages);
            aiChatProfiler.logSummary("assistant.chat.completed", elapsedMs(totalStartNs), Map.of(
                    "memoryId", memoryId,
                    "toolRounds", 0
            ));
            return response;
        }
        addMessageToWindow(messages, aiMessage);

        int rounds = 0;
        while (aiMessage.hasToolExecutionRequests()) {
            rounds++;
            if (rounds > MAX_TOOL_ROUNDS) {
                throw new IllegalStateException("Too many tool execution rounds: " + rounds);
            }

            long toolRoundStartNs = System.nanoTime();
            List<ToolExecutionResultMessage> results = executeToolRequests(aiMessage.toolExecutionRequests(), memoryId, onToolExecuted);
            aiChatProfiler.logStage("assistant.executeToolRequests.round" + rounds, elapsedMs(toolRoundStartNs), Map.of(
                    "memoryId", memoryId,
                    "toolCount", aiMessage.toolExecutionRequests().size()
            ));
            for (ToolExecutionResultMessage result : results) {
                addMessageToWindow(messages, result);
            }

            try {
                long invokeStartNs = System.nanoTime();
                response = invokeModel(messages);
                aiChatProfiler.logStage("assistant.invokeModel.round" + rounds, elapsedMs(invokeStartNs), Map.of(
                        "memoryId", memoryId,
                        "messageCount", messages.size()
                ));
            } catch (RuntimeException exception) {
                AiMessage fallbackMessage = bookingSubmitFallbackMessage(results);
                if (fallbackMessage == null) {
                    throw exception;
                }
                addMessageToWindow(messages, fallbackMessage);
                chatMemoryStore.updateMessages(memoryId, messages);
                log.warn("AI final response failed after booking submit tool; returned structured tool result", exception);
                aiChatProfiler.logSummary("assistant.chat.fallback", elapsedMs(totalStartNs), Map.of(
                        "memoryId", memoryId,
                        "toolRounds", rounds
                ));
                return Response.from(fallbackMessage);
            }
            aiMessage = response.content();
            if (aiMessage == null) {
                break;
            }
            aiMessage = appendBookingPreviewMarkerIfNeeded(aiMessage, results);
            response = Response.from(aiMessage, response.tokenUsage(), response.finishReason(), response.metadata());
            addMessageToWindow(messages, aiMessage);
        }

        chatMemoryStore.updateMessages(memoryId, messages);
        aiChatProfiler.logSummary("assistant.chat.completed", elapsedMs(totalStartNs), Map.of(
                "memoryId", memoryId,
                "toolRounds", rounds
        ));
        return response;
    }

    private AiMessage bookingSubmitFallbackMessage(List<ToolExecutionResultMessage> results) {
        for (ToolExecutionResultMessage result : results) {
            if (BOOKING_SUBMIT_TOOL_NAME.equals(result.toolName()) && result.text() != null && !result.text().isBlank()) {
                return AiMessage.from(result.text());
            }
        }
        return null;
    }

    private AiMessage appendBookingPreviewMarkerIfNeeded(AiMessage aiMessage, List<ToolExecutionResultMessage> results) {
        if (aiMessage == null || aiMessage.hasToolExecutionRequests()) {
            return aiMessage;
        }
        String bookingResult = bookingSubmitResultText(results);
        if (bookingResult == null || !bookingResult.contains("\"readyToSubmit\":true")) {
            return aiMessage;
        }

        String text = aiMessage.text() == null ? AiConstant.EMPTY_CONTENT : aiMessage.text();
        if (text.contains(BOOKING_PREVIEW_MARKER)) {
            return aiMessage;
        }
        return AiMessage.from(text + "\n\n" + BOOKING_PREVIEW_MARKER + bookingResult);
    }

    private String bookingSubmitResultText(List<ToolExecutionResultMessage> results) {
        for (ToolExecutionResultMessage result : results) {
            if (BOOKING_SUBMIT_TOOL_NAME.equals(result.toolName()) && result.text() != null && !result.text().isBlank()) {
                return result.text();
            }
        }
        return null;
    }

    private Response<AiMessage> invokeModel(List<ChatMessage> messages) {
        List<ChatMessage> sanitizedMessages = ToolArgumentSanitizer.sanitizeMessages(messages);
        Response<AiMessage> response = chatLanguageModel.generate(sanitizedMessages, toolSpecifications);
        return ToolArgumentSanitizer.sanitizeResponse(response, toolSpecifications);
    }

    private Response<AiMessage> invokeModel(List<ChatMessage> messages, ToolBox toolBox) {
        List<ChatMessage> sanitizedMessages = ToolArgumentSanitizer.sanitizeMessages(messages);
        Response<AiMessage> response = chatLanguageModel.generate(sanitizedMessages, toolBox.specifications());
        return ToolArgumentSanitizer.sanitizeResponse(response, toolBox.specifications());
    }

    private void invokeStreamingModel(
            List<ChatMessage> messages,
            StreamingResponseHandler<AiMessage> handler
    ) {
        List<ChatMessage> sanitizedMessages = ToolArgumentSanitizer.sanitizeMessages(messages);
        streamingChatLanguageModel.generate(sanitizedMessages, toolSpecifications, handler);
    }

    private void invokeStreamingModel(
            List<ChatMessage> messages,
            StreamingResponseHandler<AiMessage> handler,
            ToolBox toolBox
    ) {
        List<ChatMessage> sanitizedMessages = ToolArgumentSanitizer.sanitizeMessages(messages);
        streamingChatLanguageModel.generate(sanitizedMessages, toolBox.specifications(), handler);
    }

    private void ensureSystemMessage(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            addMessageToWindow(messages, SystemMessage.systemMessage(systemPrompt));
        }
    }

    private void addMessageToWindow(List<ChatMessage> messages, ChatMessage message) {
        if (message instanceof SystemMessage) {
            int existingSystemIndex = indexOfSystemMessage(messages);
            if (existingSystemIndex >= 0) {
                if (messages.get(existingSystemIndex).equals(message)) {
                    return;
                }
                messages.remove(existingSystemIndex);
            }
        }
        messages.add(message);
        ensureMessageWindowCapacity(messages);
    }

    private int indexOfSystemMessage(List<ChatMessage> messages) {
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index) instanceof SystemMessage) {
                return index;
            }
        }
        return -1;
    }

    private void ensureMessageWindowCapacity(List<ChatMessage> messages) {
        int maxMessages = chatMemoryProperties.getMaxMessages();
        while (messages.size() > maxMessages) {
            int removeIndex = !messages.isEmpty() && messages.get(0) instanceof SystemMessage ? 1 : 0;
            if (removeIndex >= messages.size()) {
                removeIndex = 0;
            }
            ChatMessage removed = messages.remove(removeIndex);
            if (removed instanceof AiMessage removedAiMessage && removedAiMessage.hasToolExecutionRequests()) {
                while (messages.size() > removeIndex && messages.get(removeIndex) instanceof ToolExecutionResultMessage) {
                    messages.remove(removeIndex);
                }
            }
        }
    }

    private List<ToolExecutionResultMessage> executeToolRequests(
            List<ToolExecutionRequest> requests,
            Long memoryId,
            Consumer<ToolExecution> onToolExecuted
    ) {
        return executeToolRequests(requests, memoryId, onToolExecuted, this.toolExecutors);
    }

    private List<ToolExecutionResultMessage> executeToolRequests(
            List<ToolExecutionRequest> requests,
            Long memoryId,
            Consumer<ToolExecution> onToolExecuted,
            Map<String, ToolExecutor> activeExecutors
    ) {
        Map<Integer, ToolExecutionResultMessage> resultsByIndex = new LinkedHashMap<>();
        if (requests.isEmpty()) {
            return List.of();
        }

        Set<String> readOnlyNames = parallelProperties.getReadOnlyNames();
        Map<Integer, PendingToolExecution> asyncReadOnlyByIndex = new LinkedHashMap<>();

        for (int index = 0; index < requests.size(); index++) {
            ToolExecutionRequest request = requests.get(index);
            ToolExecutor executor = activeExecutors.get(request.name());
            if (executor == null) {
                throw new IllegalStateException("No tool executor found for: " + request.name());
            }

            boolean shouldParallelize = parallelProperties.isEnabled() && readOnlyNames.contains(request.name());
            if (shouldParallelize) {
                int finalIndex = index;
                CompletableFuture<ToolExecutionResultMessage> future = CompletableFuture.supplyAsync(
                        () -> executeSingleRequest(request, executor, memoryId, onToolExecuted),
                        parallelExecutor
                );
                asyncReadOnlyByIndex.put(finalIndex, new PendingToolExecution(request, future));
                continue;
            }

            flushReadOnlyBatch(asyncReadOnlyByIndex, resultsByIndex, onToolExecuted);
            resultsByIndex.put(index, executeSingleRequest(request, executor, memoryId, onToolExecuted));
        }
        flushReadOnlyBatch(asyncReadOnlyByIndex, resultsByIndex, onToolExecuted);

        List<ToolExecutionResultMessage> ordered = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            ToolExecutionResultMessage result = resultsByIndex.get(index);
            if (result != null) {
                ordered.add(result);
            }
        }
        return ordered;
    }

    private void flushReadOnlyBatch(
            Map<Integer, PendingToolExecution> asyncReadOnlyByIndex,
            Map<Integer, ToolExecutionResultMessage> resultsByIndex,
            Consumer<ToolExecution> onToolExecuted
    ) {
        if (asyncReadOnlyByIndex.isEmpty()) {
            return;
        }
        CompletableFuture<?>[] futures = asyncReadOnlyByIndex.values().stream()
                .map(PendingToolExecution::future)
                .toArray(CompletableFuture[]::new);
        long waitStartNs = System.nanoTime();
        try {
            CompletableFuture.allOf(futures).get(parallelProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            aiChatProfiler.logStage("assistant.toolBatch.wait", elapsedMs(waitStartNs), Map.of(
                    "batchSize", asyncReadOnlyByIndex.size(),
                    "timedOut", false
            ));
            for (Map.Entry<Integer, PendingToolExecution> entry : asyncReadOnlyByIndex.entrySet()) {
                resultsByIndex.put(entry.getKey(), entry.getValue().future().join());
            }
        } catch (TimeoutException e) {
            futuresCancel(futures);
            String timeoutMessage = "Parallel tool execution timed out after " + parallelProperties.getTimeoutMs() + " ms";
            log.warn(timeoutMessage);
            aiChatProfiler.logStage("assistant.toolBatch.wait", elapsedMs(waitStartNs), Map.of(
                    "batchSize", asyncReadOnlyByIndex.size(),
                    "timedOut", true
            ));
            for (Map.Entry<Integer, PendingToolExecution> entry : asyncReadOnlyByIndex.entrySet()) {
                PendingToolExecution pending = entry.getValue();
                ToolExecutionResultMessage result;
                if (pending.future().isDone() && !pending.future().isCompletedExceptionally() && !pending.future().isCancelled()) {
                    result = pending.future().join();
                } else {
                    result = timedOutToolResult(pending.request(), timeoutMessage, onToolExecuted);
                }
                resultsByIndex.put(entry.getKey(), result);
            }
        } catch (Exception e) {
            futuresCancel(futures);
            throw unwrapParallelError(e);
        } finally {
            asyncReadOnlyByIndex.clear();
        }
    }

    private ToolExecutionResultMessage executeSingleRequest(
            ToolExecutionRequest request,
            ToolExecutor executor,
            Long memoryId,
            Consumer<ToolExecution> onToolExecuted
    ) {
        try {
            long startNs = System.nanoTime();
            String result = executor.execute(request, memoryId);
            aiChatProfiler.logStage("assistant.tool." + request.name(), elapsedMs(startNs), Map.of(
                    "toolName", request.name(),
                    "success", true
            ));
            ToolExecution toolExecution = ToolExecution.builder()
                    .request(request)
                    .result(result)
                    .build();
            onToolExecuted.accept(toolExecution);
            return ToolExecutionResultMessage.from(request, result);
        } catch (Exception exception) {
            String errorMessage = "Tool execution failed for " + request.name();
            log.warn(errorMessage, exception);
            aiChatProfiler.logEvent("assistant.tool." + request.name() + ".error", Map.of(
                    "toolName", request.name(),
                    "errorType", exception.getClass().getSimpleName(),
                    "errorMessage", rootCauseMessage(exception)
            ));
            String result = toolFailureResult(request.name(), exception);
            ToolExecution toolExecution = ToolExecution.builder()
                    .request(request)
                    .result(result)
                    .build();
            onToolExecuted.accept(toolExecution);
            return ToolExecutionResultMessage.from(request, result);
        }
    }

    private ToolExecutionResultMessage timedOutToolResult(
            ToolExecutionRequest request,
            String timeoutMessage,
            Consumer<ToolExecution> onToolExecuted
    ) {
        String result = toolFailureResult(request.name(), new TimeoutException(timeoutMessage));
        ToolExecution toolExecution = ToolExecution.builder()
                .request(request)
                .result(result)
                .build();
        onToolExecuted.accept(toolExecution);
        return ToolExecutionResultMessage.from(request, result);
    }

    private String toolFailureResult(String toolName, Exception exception) {
        try {
            if (BOOKING_SUBMIT_TOOL_NAME.equals(toolName)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", false);
                result.put("readyToSubmit", false);
                result.put("message", "Booking draft could not be prepared. Please check the selected date and time, then try again.");
                result.put("warnings", List.of(rootCauseMessage(exception)));
                return TOOL_ARGUMENT_MAPPER.writeValueAsString(result);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "Tool execution failed: " + rootCauseMessage(exception));
            return TOOL_ARGUMENT_MAPPER.writeValueAsString(result);
        } catch (Exception serializationException) {
            return "{\"success\":false,\"message\":\"Tool execution failed\"}";
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private IllegalStateException unwrapParallelError(Exception exception) {
        Throwable current = exception;
        if (current instanceof CompletionException completionException && completionException.getCause() != null) {
            current = completionException.getCause();
        }
        if (current instanceof IllegalStateException stateException) {
            return stateException;
        }
        return new IllegalStateException("Parallel tool execution failed", current);
    }

    private void futuresCancel(CompletableFuture<?>[] futures) {
        for (CompletableFuture<?> future : futures) {
            future.cancel(true);
        }
    }

    private record PendingToolExecution(
            ToolExecutionRequest request,
            CompletableFuture<ToolExecutionResultMessage> future
    ) {
    }

    @PreDestroy
    public void shutdownExecutor() {
        parallelExecutor.shutdownNow();
    }

    private List<ToolSpecification> collectSpecifications(List<Object> toolSources) {
        List<ToolSpecification> specifications = new ArrayList<>();
        for (Object source : toolSources) {
            specifications.addAll(ToolSpecifications.toolSpecificationsFrom(source));
        }
        return List.copyOf(specifications);
    }

    private Map<String, ToolExecutor> collectExecutors(List<Object> toolSources) {
        Map<String, ToolExecutor> executors = new LinkedHashMap<>();
        for (Object source : toolSources) {
            for (Method method : source.getClass().getMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null) {
                    continue;
                }
                String name = tool.name() == null || tool.name().isBlank() ? method.getName() : tool.name();
                executors.put(name, new ReflectiveToolExecutor(source, method));
            }
        }
        return Map.copyOf(executors);
    }

    private static final class ReflectiveToolExecutor implements ToolExecutor {

        private final Object source;
        private final Method method;

        private ReflectiveToolExecutor(Object source, Method method) {
            this.source = source;
            this.method = method;
        }

        @Override
        public String execute(ToolExecutionRequest request, Object memoryId) {
            try {
                Object[] args = prepareArguments(request.arguments(), method.getParameters(), memoryId);
                Object rawResult = method.invoke(source, args);
                return rawResult == null ? "null" : TOOL_ARGUMENT_MAPPER.writeValueAsString(rawResult);
            } catch (Exception exception) {
                throw new IllegalStateException("Tool invocation failed for " + request.name(), exception);
            }
        }

        private Object[] prepareArguments(String rawArguments, Parameter[] parameters, Object memoryId) {
            Map<String, Object> argumentMap = parseArguments(rawArguments);
            Object[] prepared = new Object[parameters.length];
            for (int index = 0; index < parameters.length; index++) {
                Parameter parameter = parameters[index];
                if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                    prepared[index] = memoryId;
                    continue;
                }
                Object rawValue = argumentMap.get(parameter.getName());
                prepared[index] = coerce(rawValue, parameter);
            }
            return prepared;
        }

        private Map<String, Object> parseArguments(String rawArguments) {
            if (rawArguments == null || rawArguments.isBlank()) {
                return Map.of();
            }
            try {
                return TOOL_ARGUMENT_MAPPER.readValue(rawArguments, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to parse tool arguments: " + rawArguments, exception);
            }
        }

        private Object coerce(Object rawValue, Parameter parameter) {
            if (rawValue == null) {
                if (parameter.getType().isPrimitive()) {
                    throw new IllegalStateException("Missing required primitive argument: " + parameter.getName());
                }
                return null;
            }
            try {
                Object normalizedValue = normalizeTemporalArgument(rawValue, parameter.getType());
                return TOOL_ARGUMENT_MAPPER.convertValue(normalizedValue, TOOL_ARGUMENT_MAPPER.constructType(parameter.getParameterizedType()));
            } catch (IllegalArgumentException exception) {
                P annotation = parameter.getAnnotation(P.class);
                String hint = annotation == null ? parameter.getName() : annotation.value();
                throw new IllegalStateException("Failed to coerce argument " + parameter.getName() + " (" + hint + ")", exception);
            }
        }

        private Object normalizeTemporalArgument(Object rawValue, Class<?> targetType) {
            if (LocalDate.class.equals(targetType)) {
                return normalizeLocalDateArgument(rawValue);
            }
            if (LocalTime.class.equals(targetType)) {
                return normalizeLocalTimeArgument(rawValue);
            }
            return rawValue;
        }

        private Object normalizeLocalDateArgument(Object rawValue) {
            if (rawValue instanceof String || rawValue instanceof Number) {
                return rawValue;
            }
            if (!(rawValue instanceof Map<?, ?> valueMap)) {
                return rawValue;
            }
            if (valueMap.containsKey("year") && valueMap.containsKey("month") && valueMap.containsKey("day")) {
                Object yearRaw = valueMap.get("year");
                Object monthRaw = valueMap.get("month");
                Object dayRaw = valueMap.get("day");
                if (yearRaw instanceof Number year && monthRaw instanceof Number month && dayRaw instanceof Number day) {
                    return LocalDate.of(year.intValue(), month.intValue(), day.intValue()).toString();
                }
            }
            if (valueMap.containsKey("epochDay")) {
                Object epochDayRaw = valueMap.get("epochDay");
                if (epochDayRaw instanceof Number epochDay) {
                    return LocalDate.ofEpochDay(epochDay.longValue()).toString();
                }
            }
            if (valueMap.containsKey("epochMilli")) {
                Object epochMilliRaw = valueMap.get("epochMilli");
                if (epochMilliRaw instanceof Number epochMilli) {
                    return Instant.ofEpochMilli(epochMilli.longValue())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString();
                }
            }
            return rawValue;
        }

        private Object normalizeLocalTimeArgument(Object rawValue) {
            if (rawValue instanceof String) {
                String value = rawValue.toString().trim();
                if (value.matches("^\\d{1,2}:\\d{2}$")) {
                    return value + ":00";
                }
                return value;
            }
            if (rawValue instanceof Number secondsOfDay) {
                long seconds = secondsOfDay.longValue();
                if (seconds >= 0 && seconds < 24 * 60 * 60) {
                    return LocalTime.ofSecondOfDay(seconds).toString();
                }
                return rawValue;
            }
            if (!(rawValue instanceof Map<?, ?> valueMap)) {
                return rawValue;
            }

            Object hourRaw = firstPresent(valueMap, "hour", "hours", "h");
            Object minuteRaw = firstPresent(valueMap, "minute", "minutes", "m");
            Object secondRaw = firstPresent(valueMap, "second", "seconds", "s");
            if (hourRaw instanceof Number hour && minuteRaw instanceof Number minute) {
                int second = secondRaw instanceof Number secondNumber ? secondNumber.intValue() : 0;
                return LocalTime.of(hour.intValue(), minute.intValue(), second).toString();
            }

            Object nanoOfDayRaw = valueMap.get("nanoOfDay");
            if (nanoOfDayRaw instanceof Number nanoOfDay) {
                return LocalTime.ofNanoOfDay(nanoOfDay.longValue()).toString();
            }
            Object secondOfDayRaw = valueMap.get("secondOfDay");
            if (secondOfDayRaw instanceof Number secondOfDay) {
                return LocalTime.ofSecondOfDay(secondOfDay.longValue()).toString();
            }
            return rawValue;
        }

        private Object firstPresent(Map<?, ?> valueMap, String... names) {
            for (String name : names) {
                if (valueMap.containsKey(name)) {
                    return valueMap.get(name);
                }
            }
            return null;
        }
    }

    private final class ParallelAssistantTokenStream implements TokenStream {

        private final Long memoryId;
        private final String userMessage;
        private Consumer<String> onNext = ignored -> {
        };
        private Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved = ignored -> {
        };
        private Consumer<ToolExecution> onToolExecuted = ignored -> {
        };
        private Consumer<Response<AiMessage>> onComplete = ignored -> {
        };
        private Consumer<Throwable> onError = ignored -> {
        };
        private boolean ignoreErrors;

        private ParallelAssistantTokenStream(Long memoryId, String userMessage) {
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
        public TokenStream onToolExecuted(Consumer<ToolExecution> onToolExecuted) {
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
            String traceId = AiChatTraceContext.getTraceId();
            CompletableFuture.runAsync(() -> {
                AiChatTraceContext.restoreTraceId(traceId);
                long totalStartNs = System.nanoTime();
                try {
                    onRetrieved.accept(List.of());
                    aiChatProfiler.logEvent("assistant.stream.start", Map.of(
                            "memoryId", memoryId
                    ));
                    long memoryLoadStartNs = System.nanoTime();
                    List<ChatMessage> messages = new ArrayList<>(chatMemoryStore.getMessages(memoryId));
                    aiChatProfiler.logStage("assistant.stream.memoryLoad", elapsedMs(memoryLoadStartNs), Map.of(
                            "memoryId", memoryId,
                            "messageCount", messages.size()
                    ));
                    ensureSystemMessage(messages);
                    addMessageToWindow(messages, UserMessage.userMessage(userMessage));

                    long routerStartNs = System.nanoTime();
                    AiIntent intent = intentRouterService.resolveIntent(userMessage);
                    boolean routerFallback = intent == null;
                    if (intent == null) {
                        intent = AiIntent.KNOWLEDGE;
                    }
                    ToolBox toolBox = intentToolBoxes.getOrDefault(intent, intentToolBoxes.get(AiIntent.KNOWLEDGE));
                    aiChatProfiler.logStage("assistant.stream.intentRouter", elapsedMs(routerStartNs), Map.of(
                            "memoryId", memoryId,
                            "intent", intent.name(),
                            "fallback", routerFallback
                    ));
                    aiChatProfiler.logEvent("assistant.stream.intent", Map.of(
                            "memoryId", memoryId,
                            "intent", intent.name()
                    ));

                    int rounds = 0;
                    ProbeStreamResult probeResult = runStreamingRound(
                            messages,
                            memoryId,
                            traceId,
                            totalStartNs,
                            "assistant.stream.streamingProbe",
                            true,
                            this.onNext,
                            toolBox
                    );
                    AiMessage probeAiMessage = probeResult.response().content();
                    Response<AiMessage> finalModelResponseAfterTools = null;

                    while (probeAiMessage != null && probeAiMessage.hasToolExecutionRequests()) {
                        addMessageToWindow(messages, probeAiMessage);
                        rounds++;
                        if (rounds > MAX_TOOL_ROUNDS) {
                            throw new IllegalStateException("Too many tool execution rounds: " + rounds);
                        }

                        long toolRoundStartNs = System.nanoTime();
                        List<ToolExecutionResultMessage> results =
                                executeToolRequests(probeAiMessage.toolExecutionRequests(), memoryId, onToolExecuted, toolBox.executors());
                        aiChatProfiler.logStage("assistant.stream.executeToolRequests.round" + rounds, elapsedMs(toolRoundStartNs), Map.of(
                                "memoryId", memoryId,
                                "toolCount", probeAiMessage.toolExecutionRequests().size()
                        ));
                        for (ToolExecutionResultMessage result : results) {
                            addMessageToWindow(messages, result);
                        }

                        long invokeStartNs = System.nanoTime();
                        Response<AiMessage> probeResponse = invokeModel(messages, toolBox);
                        aiChatProfiler.logStage("assistant.stream.invokeModel.round" + rounds, elapsedMs(invokeStartNs), Map.of(
                                "memoryId", memoryId,
                                "messageCount", messages.size()
                        ));
                        probeAiMessage = probeResponse.content();
                        if (probeAiMessage != null && !probeAiMessage.hasToolExecutionRequests()) {
                            finalModelResponseAfterTools = probeResponse;
                        }
                    }

                    aiChatProfiler.logStage("assistant.stream.preStreamingTotal", elapsedMs(totalStartNs), Map.of(
                            "memoryId", memoryId,
                            "toolRounds", rounds
                    ));

                    Response<AiMessage> streamedResponse;
                    if (rounds == 0) {
                        streamedResponse = probeResult.response();
                    } else if (finalModelResponseAfterTools != null && finalModelResponseAfterTools.content() != null
                            && finalModelResponseAfterTools.content().text() != null
                            && !finalModelResponseAfterTools.content().text().isBlank()) {
                        AiMessage finalAiMessage = finalModelResponseAfterTools.content();
                        emitTextChunks(finalAiMessage.text(), this.onNext, traceId, totalStartNs, memoryId);
                        streamedResponse = finalModelResponseAfterTools;
                    } else {
                        ProbeStreamResult finalAnswerResult = runStreamingRound(
                                messages,
                                memoryId,
                                traceId,
                                totalStartNs,
                                "assistant.stream.streamingAnswer",
                                true,
                                this.onNext,
                                toolBox
                        );
                        streamedResponse = finalAnswerResult.response();
                    }

                    AiMessage streamedAiMessage = streamedResponse.content();
                    if (streamedAiMessage != null) {
                        addMessageToWindow(messages, streamedAiMessage);
                    }
                    chatMemoryStore.updateMessages(memoryId, messages);
                    aiChatProfiler.logSummary("assistant.stream.completed", elapsedMs(totalStartNs), Map.of(
                            "memoryId", memoryId,
                            "toolRounds", rounds,
                            "finalMessageCount", messages.size()
                    ));
                    onComplete.accept(streamedResponse);
                } catch (Throwable throwable) {
                    if (ignoreErrors) {
                        return;
                    }
                    aiChatProfiler.logSummary("assistant.stream.failed", elapsedMs(totalStartNs), Map.of(
                            "memoryId", memoryId,
                            "errorType", throwable.getClass().getSimpleName(),
                            "errorMessage", rootCauseMessage(throwable)
                    ));
                    onError.accept(throwable);
                }
            });
        }
    }

    private ProbeStreamResult runStreamingRound(
            List<ChatMessage> messages,
            Long memoryId,
            String traceId,
            long totalStartNs,
            String stageName,
            boolean emitTokens,
            Consumer<String> tokenConsumer,
            ToolBox toolBox
    ) {
        AtomicReference<Response<AiMessage>> streamedResponseRef = new AtomicReference<>();
        CompletableFuture<Response<AiMessage>> streamedResponseFuture = new CompletableFuture<>();
        long streamingInvokeStartNs = System.nanoTime();
        AtomicReference<Boolean> firstTokenLogged = new AtomicReference<>(Boolean.FALSE);

        invokeStreamingModel(messages, new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                AiChatTraceContext.restoreTraceId(traceId);
                if (firstTokenLogged.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
                    aiChatProfiler.logStage("assistant.stream.timeToFirstToken", elapsedMs(totalStartNs), Map.of(
                            "memoryId", memoryId,
                            "streamStage", stageName
                    ));
                }
                if (emitTokens) {
                    tokenConsumer.accept(token);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                AiChatTraceContext.restoreTraceId(traceId);
                streamedResponseRef.set(response);
                streamedResponseFuture.complete(response);
            }

            @Override
            public void onError(Throwable error) {
                AiChatTraceContext.restoreTraceId(traceId);
                streamedResponseFuture.completeExceptionally(error);
            }
        }, toolBox);

        aiChatProfiler.logStage(stageName, elapsedMs(streamingInvokeStartNs), Map.of(
                "memoryId", memoryId
        ));

        Response<AiMessage> response = streamedResponseFuture.join();
        return new ProbeStreamResult(response);
    }

    private void emitTextChunks(
            String text,
            Consumer<String> tokenConsumer,
            String traceId,
            long totalStartNs,
            Long memoryId
    ) {
        if (text == null || text.isBlank()) {
            return;
        }
        boolean firstChunk = true;
        for (int index = 0; index < text.length(); index += STREAM_CHUNK_SIZE) {
            AiChatTraceContext.restoreTraceId(traceId);
            if (firstChunk) {
                aiChatProfiler.logStage("assistant.stream.timeToFirstToken", elapsedMs(totalStartNs), Map.of(
                        "memoryId", memoryId,
                        "streamStage", "assistant.stream.finalModelText"
                ));
                firstChunk = false;
            }
            int endIndex = Math.min(index + STREAM_CHUNK_SIZE, text.length());
            tokenConsumer.accept(text.substring(index, endIndex));
        }
    }

    private record ProbeStreamResult(
            Response<AiMessage> response
    ) {
    }

    private Map<AiIntent, ToolBox> collectIntentToolBoxes(Map<AiIntent, List<Object>> toolGroups) {
        Map<AiIntent, ToolBox> map = new EnumMap<>(AiIntent.class);
        for (AiIntent intent : AiIntent.values()) {
            List<Object> sources = toolGroups == null ? List.of() : toolGroups.getOrDefault(intent, List.of());
            map.put(intent, new ToolBox(collectSpecifications(sources), collectExecutors(sources)));
        }
        return Map.copyOf(map);
    }

    private record ToolBox(
            List<ToolSpecification> specifications,
            Map<String, ToolExecutor> executors
    ) {
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }
}
