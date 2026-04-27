package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiChatMemoryProperties;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiToolParallelProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
    void shouldReturnToolFailureResultWhenAnyParallelToolFails() {
        TestReadOnlyTools tools = new TestReadOnlyTools(true);
        Assistant assistant = buildAssistant(tools, Setups.defaultParallelProperties(), new TwoStepToolThenAnswerModel(
                List.of(
                        request("id-1", "searchCurrentCustomerBookings"),
                        request("id-2", "searchKnowledgeBase")
                )
        ));

        assertThat(assistant.chat(1001L, "query")).isEqualTo("done");
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
    void shouldCoerceLocalDateFromObjectArgument() {
        DateObjectTool tool = new DateObjectTool();
        Assistant assistant = buildAssistant(tool, Setups.defaultParallelProperties(), new TwoStepToolThenAnswerModel(
                List.of(request("id-date", "searchCurrentCustomerBookings", "{\"startDate\":{\"year\":2026,\"month\":4,\"day\":22}}"))
        ));

        String reply = assistant.chat(1001L, "query");

        assertThat(reply).isEqualTo("done");
        assertThat(tool.receivedStartDate()).isEqualTo("2026-04-22");
    }

    private Assistant buildAssistant(Object toolSource, AiToolParallelProperties parallelProperties, ChatLanguageModel model) {
        AiChatMemoryProperties memoryProperties = new AiChatMemoryProperties();
        InMemoryStore store = new InMemoryStore();
        StreamingChatLanguageModel streaming = mock(StreamingChatLanguageModel.class);
        return new ParallelToolAssistant(
                model,
                streaming,
                store,
                memoryProperties,
                parallelProperties,
                "system",
                List.of(toolSource)
        );
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
