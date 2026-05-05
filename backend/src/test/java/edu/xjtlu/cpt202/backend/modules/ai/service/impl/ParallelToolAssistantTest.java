package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiChatMemoryProperties;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiIntentRouterProperties;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiToolParallelProperties;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService.FallbackReason;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService.IntentDecision;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParallelToolAssistantTest {

    @Test
    void shouldExecuteTwoReadOnlyToolsInParallel() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        Assistant assistant = buildAssistant(tools, Setups.defaultParallelProperties(), new TwoStepToolThenAnswerModel(
                List.of(
                        request("id-1", "searchCurrentCustomerBookings"),
                        request("id-2", "searchKnowledgeBase")
                )
        ));

        long start = System.currentTimeMillis();
        String reply = assistant.chat(1001L, "query");
        long duration = System.currentTimeMillis() - start;

        assertThat(reply).isEqualTo("done");
        assertThat(duration).isLessThan(900);
        assertThat(tools.executionThreads()).hasSize(2);
    }

    @Test
    void shouldKeepOriginalRequestOrderWhenCollectingToolResults() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        CaptureToolResultOrderModel model = new CaptureToolResultOrderModel(List.of(
                request("id-a", "searchCurrentCustomerBookings"),
                request("id-b", "searchKnowledgeBase")
        ));
        Assistant assistant = buildAssistant(tools, Setups.defaultParallelProperties(), model);

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(model.toolResultIds()).containsExactly("id-a", "id-b");
    }

    @Test
    void shouldConvertParallelToolFailureIntoToolResult() {
        TestReadOnlyTools tools = new TestReadOnlyTools(true);
        CaptureToolResultTextsModel model = new CaptureToolResultTextsModel(List.of(
                request("id-1", "searchCurrentCustomerBookings"),
                request("id-2", "searchKnowledgeBase")
        ));
        Assistant assistant = buildAssistant(tools, Setups.defaultParallelProperties(), model);

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(model.toolResultTexts()).hasSize(2);
        assertThat(model.toolResultTexts().get(0)).contains("bookings");
        assertThat(model.toolResultTexts().get(1)).contains("\"success\":false");
        assertThat(model.toolResultTexts().get(1)).contains("forced");
    }

    @Test
    void shouldRunWriteToolSerialAndNotInParallelPool() {
        MixedTools mixedTools = new MixedTools();
        AiToolParallelProperties parallelProperties = Setups.defaultParallelProperties();
        parallelProperties.setReadOnlyNames(new LinkedHashSet<>(List.of(
                "searchCurrentCustomerBookings",
                "searchKnowledgeBase"
        )));
        CaptureToolResultOrderModel model = new CaptureToolResultOrderModel(List.of(
                request("id-a", "searchCurrentCustomerBookings"),
                request("id-b", "submitCurrentCustomerBooking"),
                request("id-c", "searchKnowledgeBase")
        ));
        Assistant assistant = buildAssistant(mixedTools, parallelProperties, model);

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(mixedTools.writeThreadName()).doesNotContain("ai-tool-parallel-");
        assertThat(model.toolResultIds()).containsExactly("id-a", "id-b", "id-c");
    }

    @Test
    void shouldConvertParallelTimeoutIntoToolFailureResult() {
        SlowReadOnlyTools tools = new SlowReadOnlyTools();
        AiToolParallelProperties parallelProperties = Setups.defaultParallelProperties();
        parallelProperties.setTimeoutMs(100L);
        CaptureToolResultTextsModel model = new CaptureToolResultTextsModel(List.of(
                request("id-a", "searchCurrentCustomerBookings"),
                request("id-b", "searchKnowledgeBase")
        ));
        Assistant assistant = buildAssistant(tools, parallelProperties, model);

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(model.toolResultTexts()).hasSize(2);
        assertThat(model.toolResultTexts().get(0)).contains("\"success\":false");
        assertThat(model.toolResultTexts().get(0)).contains("timed out");
        assertThat(model.toolResultTexts().get(1)).contains("\"success\":false");
        assertThat(model.toolResultTexts().get(1)).contains("timed out");
    }

    @Test
    void shouldCoerceLocalDateFromObjectArgument() {
        DateObjectTool tool = new DateObjectTool();
        Assistant assistant = buildAssistant(tool, Setups.defaultParallelProperties(), new TwoStepToolThenAnswerModel(
                List.of(request("id-date", "searchCurrentCustomerBookings", "{\"startDate\":{\"year\":2026,\"month\":4,\"day\":22}}"))
        ));

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(tool.receivedStartDate()).isEqualTo("2026-04-22");
    }

    @Test
    void shouldStreamDirectlyWithoutSyncProbeWhenNoToolsNeeded() throws InterruptedException {
        NoToolModel model = new NoToolModel();
        RecordingStreamingModel streamingModel = new RecordingStreamingModel(List.of("hello", " world"), AiMessage.from("hello world"));
        Assistant assistant = buildAssistant(new TestReadOnlyTools(false), Setups.defaultParallelProperties(), model, streamingModel);

        List<String> chunks = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        TokenStream tokenStream = assistant.streamChat(1001L, "query");
        tokenStream.onNext(chunks::add)
                .onComplete(response -> latch.countDown())
                .onError(error -> latch.countDown())
                .start();

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertThat(model.invocationCount()).isZero();
        assertThat(streamingModel.invocationCount()).isEqualTo(1);
        assertThat(chunks).containsExactly("hello", " world");
    }

    @Test
    void shouldExecuteToolsAndThenStreamFinalAnswer() throws InterruptedException {
        SingleToolRoundModel model = new SingleToolRoundModel(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        ));
        RecordingStreamingModel streamingModel = new RecordingStreamingModel(List.of(), AiMessage.from(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        )));
        streamingModel.enqueue(List.of("done"), AiMessage.from("done"));
        Assistant assistant = buildAssistant(new TestReadOnlyTools(false), Setups.defaultParallelProperties(), model, streamingModel);

        List<String> chunks = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        TokenStream tokenStream = assistant.streamChat(1001L, "query");
        tokenStream.onNext(chunks::add)
                .onComplete(response -> latch.countDown())
                .onError(error -> latch.countDown())
                .start();

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertThat(model.invocationCount()).isEqualTo(1);
        assertThat(streamingModel.invocationCount()).isEqualTo(1);
        assertThat(chunks).containsExactly("done");
    }

    @Test
    void shouldEmitSyncFinalAnswerAfterToolExecutionWithoutRestartingStreaming() throws InterruptedException {
        SingleToolRoundModel model = new SingleToolRoundModel(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        ));
        RecordingStreamingModel streamingModel = new RecordingStreamingModel(List.of(), AiMessage.from(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        )));
        Assistant assistant = buildAssistant(new TestReadOnlyTools(false), Setups.defaultParallelProperties(), model, streamingModel);

        List<String> chunks = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        TokenStream tokenStream = assistant.streamChat(1001L, "query");
        tokenStream.onNext(chunks::add)
                .onComplete(response -> latch.countDown())
                .onError(error -> latch.countDown())
                .start();

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertThat(model.invocationCount()).isEqualTo(1);
        assertThat(streamingModel.invocationCount()).isEqualTo(1);
        assertThat(String.join("", chunks)).isEqualTo("done");
    }

    @Test
    void shouldInjectDashboardToolBoxForStreaming() throws InterruptedException {
        IntentAwareToolSelectionModel model = new IntentAwareToolSelectionModel(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        ));
        RecordingStreamingModel streamingModel = new RecordingStreamingModel(List.of(), AiMessage.from(List.of(
                request("id-1", "searchCurrentCustomerBookings")
        )));
        CombinedTools combinedTools = new CombinedTools();
        Assistant assistant = buildAssistantWithGroups(
                combinedTools,
                Setups.defaultParallelProperties(),
                model,
                streamingModel,
                (memoryId, message) -> AiIntent.DASHBOARD,
                groupedToolsForCombined(combinedTools)
        );

        CountDownLatch latch = new CountDownLatch(1);
        assistant.streamChat(1001L, "query")
                .onNext(ignored -> { })
                .onComplete(response -> latch.countDown())
                .onError(error -> latch.countDown())
                .start();

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertThat(model.capturedToolNames()).contains("searchCurrentCustomerBookings");
        assertThat(model.capturedToolNames()).doesNotContain("searchKnowledgeBase");
        assertThat(model.capturedToolNames()).doesNotContain("submitCurrentCustomerBooking");
    }

    @Test
    void shouldExtractOriginalUserMessageWhenSystemTimePrefixExists() {
        String wrappedMessage = """
                Current system time: 2026-05-03 00:00:00 CST
                Use this as the authoritative current time when interpreting relative dates such as today, tomorrow, upcoming, this week, and history.

                User message:
                show my bookings history
                """;

        String extracted = ParallelToolAssistant.extractOriginalUserMessage(wrappedMessage);
        assertThat(extracted).isEqualTo("show my bookings history");
    }

    @Test
    void shouldUseRuleBasedChitchatIntentWithoutCallingModel() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        LightModelAiIntentRouterService routerService = new LightModelAiIntentRouterService(model, properties);

        AiIntent intent = routerService.resolveIntent(1001L, "hello");

        assertThat(intent).isEqualTo(AiIntent.CHITCHAT);
        verify(model, times(0)).generate(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldResolveBookingForActionLikeBookingQuestionWhenKnowledgeHintsAreNarrowed() {
        CountingIntentModel model = new CountingIntentModel("BOOKING");
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        LightModelAiIntentRouterService routerService = new LightModelAiIntentRouterService(model, properties);

        AiIntent first = routerService.resolveIntent(1001L, "Need help with complex expert selection");
        AiIntent second = routerService.resolveIntent(1001L, "Need help with complex expert selection");

        assertThat(first).isEqualTo(AiIntent.BOOKING);
        assertThat(second).isEqualTo(AiIntent.BOOKING);
        assertThat(model.invocationCount()).isEqualTo(2);
    }

    @Test
    void shouldPreferKnowledgeForPolicyLikeBookingQuestion() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        LightModelAiIntentRouterService routerService = new LightModelAiIntentRouterService(model, properties);

        AiIntent intent = routerService.resolveIntent(1001L,
                "If I book a specialist but need to change the time later, will it be Confirmed automatically?");

        assertThat(intent).isEqualTo(AiIntent.KNOWLEDGE);
    }

    @Test
    void shouldResolveCancelIntentForCancelBookingRequest() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        LightModelAiIntentRouterService routerService = new LightModelAiIntentRouterService(model, properties);

        AiIntent intent = routerService.resolveIntent(1001L, "Please cancel my booking for tomorrow");

        assertThat(intent).isEqualTo(AiIntent.CANCEL);
        verify(model, times(0)).generate(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldPreferKnowledgeForUnsupportedCharactersQuestion() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        LightModelAiIntentRouterService routerService = new LightModelAiIntentRouterService(model, properties);

        AiIntent intent = routerService.resolveIntent(1001L,
                "I submitted my notes, but it says 'unsupported characters'. What should I do?");

        assertThat(intent).isEqualTo(AiIntent.KNOWLEDGE);
    }

    @Test
    void shouldReturnFinalAnswerFromCacheWhenKnowledgeCacheHits() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
        when(semanticCacheService.get("refund policy", AiIntent.KNOWLEDGE))
                .thenReturn(java.util.Optional.of(new AiSemanticCacheService.CacheHit("cache-1", "cached final answer", true, 1.0D)));
        Assistant assistant = buildAssistantWithCache(
                tools,
                Setups.defaultParallelProperties(),
                new TwoStepToolThenAnswerModel(List.of(request("id-1", "searchKnowledgeBase"))),
                semanticCacheService,
                (memoryId, message) -> AiIntent.KNOWLEDGE
        );

        String reply = assistant.chat(1001L, "User message:\nrefund policy");

        assertThat(reply).isEqualTo("cached final answer");
        assertThat(tools.executionThreads()).isEmpty();
        verify(semanticCacheService, never()).putAsync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
    }

    @Test
    void shouldWriteFinalAnswerToCacheWhenKnowledgeMisses() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
        when(semanticCacheService.get("refund policy", AiIntent.KNOWLEDGE))
                .thenReturn(java.util.Optional.empty());
        Assistant assistant = buildAssistantWithCache(
                tools,
                Setups.defaultParallelProperties(),
                new TwoStepToolThenAnswerModel(List.of(request("id-1", "searchKnowledgeBase"))),
                semanticCacheService,
                (memoryId, message) -> AiIntent.KNOWLEDGE
        );

        String reply = assistant.chat(1001L, "User message:\nrefund policy");

        assertThat(reply).isEqualTo("done");
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(semanticCacheService).putAsync("refund policy", "done", AiIntent.KNOWLEDGE)
        );
    }

    @Test
    void shouldNotUseKnowledgeCacheForNonKnowledgeIntent() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
        Assistant assistant = buildAssistantWithCache(
                tools,
                Setups.defaultParallelProperties(),
                new TwoStepToolThenAnswerModel(List.of(request("id-1", "searchKnowledgeBase"))),
                semanticCacheService,
                (memoryId, message) -> AiIntent.DASHBOARD
        );

        String reply = assistant.chat(1001L, "User message:\nmy dashboard");

        assertThat(reply).isEqualTo("done");
        verify(semanticCacheService, never()).get(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
        verify(semanticCacheService, never()).putAsync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
    }

    @Test
    void shouldNotCacheKnowledgeFallbackMessage() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
        when(semanticCacheService.get("refund policy", AiIntent.KNOWLEDGE)).thenReturn(java.util.Optional.empty());
        Assistant assistant = buildAssistantWithCache(
                tools,
                Setups.defaultParallelProperties(),
                new FixedAnswerModel(AiConstant.KNOWLEDGE_NOT_FOUND_FALLBACK_MESSAGE),
                semanticCacheService,
                (memoryId, message) -> AiIntent.KNOWLEDGE
        );

        String reply = assistant.chat(1001L, "User message:\nrefund policy");

        assertThat(reply).isEqualTo(AiConstant.KNOWLEDGE_NOT_FOUND_FALLBACK_MESSAGE);
        verify(semanticCacheService, never()).putAsync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
    }

    @Test
    void shouldNotReadOrWriteKnowledgeCacheWhenIntentIsTimeoutFallbackKnowledge() {
        TestReadOnlyTools tools = new TestReadOnlyTools(false);
        AiSemanticCacheService semanticCacheService = mock(AiSemanticCacheService.class);
        Assistant assistant = buildAssistantWithCache(
                tools,
                Setups.defaultParallelProperties(),
                new TwoStepToolThenAnswerModel(List.of(request("id-1", "searchKnowledgeBase"))),
                semanticCacheService,
                new FixedIntentRouterService(new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.TIMEOUT_FALLBACK))
        );

        String reply = assistant.chat(1001L, "User message:\nrefund policy");

        assertThat(reply).isEqualTo("done");
        verify(semanticCacheService, never()).get(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
        verify(semanticCacheService, never()).putAsync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(AiIntent.KNOWLEDGE));
    }

    private Assistant buildAssistant(Object toolSource, AiToolParallelProperties parallelProperties, ChatLanguageModel model) {
        return buildAssistantWithCache(toolSource, parallelProperties, model, mock(AiSemanticCacheService.class), (memoryId, message) -> AiIntent.DASHBOARD);
    }

    private Assistant buildAssistantWithCache(
            Object toolSource,
            AiToolParallelProperties parallelProperties,
            ChatLanguageModel model,
            AiSemanticCacheService semanticCacheService,
            AiIntentRouterService routerService
    ) {
        AiChatMemoryProperties memoryProperties = new AiChatMemoryProperties();
        InMemoryStore store = new InMemoryStore();
        StreamingChatLanguageModel streaming = mock(StreamingChatLanguageModel.class);
        AiChatProfiler aiChatProfiler = new AiChatProfiler(new CommonProperties());
        return new ParallelToolAssistant(
                model,
                streaming,
                store,
                memoryProperties,
                parallelProperties,
                aiChatProfiler,
                "system",
                List.of(toolSource),
                routerService,
                defaultGroupedTools(toolSource),
                semanticCacheService
        );
    }

    private Assistant buildAssistant(
            Object toolSource,
            AiToolParallelProperties parallelProperties,
            ChatLanguageModel model,
            StreamingChatLanguageModel streamingModel
    ) {
        AiChatMemoryProperties memoryProperties = new AiChatMemoryProperties();
        InMemoryStore store = new InMemoryStore();
        AiChatProfiler aiChatProfiler = new AiChatProfiler(new CommonProperties());
        return new ParallelToolAssistant(
                model,
                streamingModel,
                store,
                memoryProperties,
                parallelProperties,
                aiChatProfiler,
                "system",
                List.of(toolSource),
                (memoryId, message) -> AiIntent.DASHBOARD,
                defaultGroupedTools(toolSource),
                mock(AiSemanticCacheService.class)
        );
    }

    private Assistant buildAssistantWithGroups(
            Object toolSource,
            AiToolParallelProperties parallelProperties,
            ChatLanguageModel model,
            StreamingChatLanguageModel streamingModel,
            AiIntentRouterService routerService,
            Map<AiIntent, List<Object>> groups
    ) {
        AiChatMemoryProperties memoryProperties = new AiChatMemoryProperties();
        InMemoryStore store = new InMemoryStore();
        AiChatProfiler aiChatProfiler = new AiChatProfiler(new CommonProperties());
        return new ParallelToolAssistant(
                model,
                streamingModel,
                store,
                memoryProperties,
                parallelProperties,
                aiChatProfiler,
                "system",
                List.of(toolSource),
                routerService,
                groups,
                mock(AiSemanticCacheService.class)
        );
    }

    private Map<AiIntent, List<Object>> defaultGroupedTools(Object toolSource) {
        Map<AiIntent, List<Object>> groups = new EnumMap<>(AiIntent.class);
        groups.put(AiIntent.KNOWLEDGE, List.of(toolSource));
        groups.put(AiIntent.BOOKING, List.of(toolSource));
        groups.put(AiIntent.DASHBOARD, List.of(toolSource));
        return groups;
    }

    private Map<AiIntent, List<Object>> groupedToolsForCombined(CombinedTools combinedTools) {
        Map<AiIntent, List<Object>> groups = new EnumMap<>(AiIntent.class);
        groups.put(AiIntent.KNOWLEDGE, List.of(new KnowledgeOnlyToolProxy(combinedTools)));
        groups.put(AiIntent.BOOKING, List.of(new BookingOnlyToolProxy(combinedTools)));
        groups.put(AiIntent.DASHBOARD, List.of(new DashboardOnlyToolProxy(combinedTools)));
        return groups;
    }

    private static ToolExecutionRequest request(String id, String name) {
        return ToolExecutionRequest.builder()
                .id(id)
                .name(name)
                .arguments("{}")
                .build();
    }

    private static ToolExecutionRequest request(String id, String name, String arguments) {
        return ToolExecutionRequest.builder()
                .id(id)
                .name(name)
                .arguments(arguments)
                .build();
    }

    private static class Setups {
        private static AiToolParallelProperties defaultParallelProperties() {
            AiToolParallelProperties properties = new AiToolParallelProperties();
            properties.setEnabled(true);
            properties.setMaxConcurrency(4);
            properties.setTimeoutMs(8_000L);
            properties.setReadOnlyNames(new LinkedHashSet<>(List.of(
                    "searchCurrentCustomerBookings",
                    "searchKnowledgeBase"
            )));
            return properties;
        }
    }

    private static class TwoStepToolThenAnswerModel implements ChatLanguageModel {
        private final List<ToolExecutionRequest> firstRoundRequests;
        private int invocationCount = 0;

        private TwoStepToolThenAnswerModel(List<ToolExecutionRequest> firstRoundRequests) {
            this.firstRoundRequests = firstRoundRequests;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            invocationCount++;
            if (invocationCount == 1) {
                return Response.from(AiMessage.from(firstRoundRequests));
            }
            return Response.from(AiMessage.from("done"));
        }

        private int invocationCount() {
            return invocationCount;
        }
    }

    private static class NoToolModel implements ChatLanguageModel {
        private int invocationCount = 0;

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            invocationCount++;
            return Response.from(AiMessage.from("unused"));
        }

        private int invocationCount() {
            return invocationCount;
        }
    }

    private static class SingleToolRoundModel implements ChatLanguageModel {
        private final List<ToolExecutionRequest> firstRoundRequests;
        private int invocationCount = 0;

        private SingleToolRoundModel(List<ToolExecutionRequest> firstRoundRequests) {
            this.firstRoundRequests = firstRoundRequests;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            invocationCount++;
            long resultCount = messages.stream()
                    .filter(message -> message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage)
                    .count();
            if (resultCount > 0) {
                return Response.from(AiMessage.from("done"));
            }
            return Response.from(AiMessage.from(firstRoundRequests));
        }

        private int invocationCount() {
            return invocationCount;
        }
    }

    private static class RecordingStreamingModel implements StreamingChatLanguageModel {
        private final List<StreamingRound> rounds = new ArrayList<>();
        private int invocationCount = 0;

        private RecordingStreamingModel(List<String> tokens, AiMessage message) {
            rounds.add(new StreamingRound(tokens, message));
        }

        private void enqueue(List<String> tokens, AiMessage message) {
            rounds.add(new StreamingRound(tokens, message));
        }

        @Override
        public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
            StreamingRound round = rounds.get(invocationCount++);
            for (String token : round.tokens()) {
                handler.onNext(token);
            }
            handler.onComplete(Response.from(round.message()));
        }

        @Override
        public void generate(
                List<ChatMessage> messages,
                List<ToolSpecification> toolSpecifications,
                StreamingResponseHandler<AiMessage> handler
        ) {
            generate(messages, handler);
        }

        private int invocationCount() {
            return invocationCount;
        }
    }

    private static class IntentAwareToolSelectionModel implements ChatLanguageModel {
        private final List<ToolExecutionRequest> firstRoundRequests;
        private List<String> capturedToolNames = List.of();
        private int invocationCount = 0;

        private IntentAwareToolSelectionModel(List<ToolExecutionRequest> firstRoundRequests) {
            this.firstRoundRequests = firstRoundRequests;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            invocationCount++;
            capturedToolNames = toolSpecifications.stream().map(ToolSpecification::name).toList();
            if (invocationCount == 1) {
                return Response.from(AiMessage.from(firstRoundRequests));
            }
            return Response.from(AiMessage.from("done"));
        }

        private List<String> capturedToolNames() {
            return capturedToolNames;
        }
    }

    private static class CountingIntentModel implements ChatLanguageModel {
        private final String responseText;
        private int invocationCount;

        private CountingIntentModel(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            invocationCount++;
            return Response.from(AiMessage.from(responseText));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            invocationCount++;
            return Response.from(AiMessage.from(responseText));
        }

        private int invocationCount() {
            return invocationCount;
        }
    }

    private static class FixedIntentRouterService implements AiIntentRouterService {

        private final IntentDecision decision;

        private FixedIntentRouterService(IntentDecision decision) {
            this.decision = decision;
        }

        @Override
        public IntentDecision resolveIntentDecision(Long memoryId, String userMessage) {
            return decision;
        }
    }

    private static class FixedAnswerModel implements ChatLanguageModel {
        private final String answer;

        private FixedAnswerModel(String answer) {
            this.answer = answer;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return Response.from(AiMessage.from(answer));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            return Response.from(AiMessage.from(answer));
        }
    }

    private static class CombinedTools {
        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings() {
            return "bookings";
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchKnowledgeBase() {
            return "knowledge";
        }

        @dev.langchain4j.agent.tool.Tool
        public String submitCurrentCustomerBooking() {
            return "submit";
        }
    }

    private static class KnowledgeOnlyToolProxy {
        private final CombinedTools delegate;

        private KnowledgeOnlyToolProxy(CombinedTools delegate) {
            this.delegate = delegate;
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchKnowledgeBase() {
            return delegate.searchKnowledgeBase();
        }
    }

    private static class BookingOnlyToolProxy {
        private final CombinedTools delegate;

        private BookingOnlyToolProxy(CombinedTools delegate) {
            this.delegate = delegate;
        }

        @dev.langchain4j.agent.tool.Tool
        public String submitCurrentCustomerBooking() {
            return delegate.submitCurrentCustomerBooking();
        }
    }

    private static class DashboardOnlyToolProxy {
        private final CombinedTools delegate;

        private DashboardOnlyToolProxy(CombinedTools delegate) {
            this.delegate = delegate;
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings() {
            return delegate.searchCurrentCustomerBookings();
        }
    }

    private record StreamingRound(List<String> tokens, AiMessage message) {
    }

    private static class CaptureToolResultOrderModel extends TwoStepToolThenAnswerModel {
        private final List<String> toolResultIds = new ArrayList<>();

        private CaptureToolResultOrderModel(List<ToolExecutionRequest> firstRoundRequests) {
            super(firstRoundRequests);
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            long resultCount = messages.stream()
                    .filter(message -> message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage)
                    .count();
            if (resultCount > 0) {
                messages.stream()
                        .filter(message -> message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage)
                        .map(dev.langchain4j.data.message.ToolExecutionResultMessage.class::cast)
                        .map(dev.langchain4j.data.message.ToolExecutionResultMessage::id)
                        .forEach(toolResultIds::add);
            }
            return super.generate(messages, toolSpecifications);
        }

        private List<String> toolResultIds() {
            return toolResultIds;
        }
    }

    private static class CaptureToolResultTextsModel extends TwoStepToolThenAnswerModel {
        private final List<String> toolResultTexts = new ArrayList<>();

        private CaptureToolResultTextsModel(List<ToolExecutionRequest> firstRoundRequests) {
            super(firstRoundRequests);
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            messages.stream()
                    .filter(message -> message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage)
                    .map(dev.langchain4j.data.message.ToolExecutionResultMessage.class::cast)
                    .map(dev.langchain4j.data.message.ToolExecutionResultMessage::text)
                    .forEach(toolResultTexts::add);
            return super.generate(messages, toolSpecifications);
        }

        private List<String> toolResultTexts() {
            return toolResultTexts;
        }
    }

    private static class TestReadOnlyTools {
        private final boolean failKnowledgeTool;
        private final ConcurrentLinkedQueue<String> executionThreads = new ConcurrentLinkedQueue<>();

        private TestReadOnlyTools(boolean failKnowledgeTool) {
            this.failKnowledgeTool = failKnowledgeTool;
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings() throws InterruptedException {
            Thread.sleep(500);
            executionThreads.add(Thread.currentThread().getName());
            return "bookings";
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchKnowledgeBase() throws InterruptedException {
            Thread.sleep(500);
            executionThreads.add(Thread.currentThread().getName());
            if (failKnowledgeTool) {
                throw new IllegalStateException("forced");
            }
            return "knowledge";
        }

        private List<String> executionThreads() {
            return List.copyOf(executionThreads);
        }
    }

    private static class MixedTools {
        private volatile String writeThreadName;

        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings() throws InterruptedException {
            Thread.sleep(300);
            return "bookings";
        }

        @dev.langchain4j.agent.tool.Tool
        public String submitCurrentCustomerBooking() {
            writeThreadName = Thread.currentThread().getName();
            return "submitted";
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchKnowledgeBase() throws InterruptedException {
            Thread.sleep(300);
            return "knowledge";
        }

        private String writeThreadName() {
            return writeThreadName;
        }
    }

    private static class SlowReadOnlyTools {

        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings() throws InterruptedException {
            Thread.sleep(500);
            return "bookings";
        }

        @dev.langchain4j.agent.tool.Tool
        public String searchKnowledgeBase() throws InterruptedException {
            Thread.sleep(500);
            return "knowledge";
        }
    }

    private static class InMemoryStore implements ChatMemoryStore {
        private final Map<Object, List<ChatMessage>> messages = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return messages.getOrDefault(memoryId, List.of());
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            this.messages.put(memoryId, List.copyOf(messages));
        }

        @Override
        public void deleteMessages(Object memoryId) {
            messages.remove(memoryId);
        }
    }

    private static class DateObjectTool {
        private volatile String receivedStartDate;

        @dev.langchain4j.agent.tool.Tool
        public String searchCurrentCustomerBookings(java.time.LocalDate startDate) {
            receivedStartDate = startDate == null ? null : startDate.toString();
            return "ok";
        }

        private String receivedStartDate() {
            return receivedStartDate;
        }
    }
}
