package edu.xjtlu.cpt202.backend.modules.ai.util;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolArgumentSanitizerTest {

    @Test
    void shouldIgnorePlainTextAiMessageWithoutToolRequests() {
        Response<AiMessage> response = Response.from(
                AiMessage.aiMessage("hello"),
                null,
                FinishReason.STOP,
                Map.of()
        );

        Response<AiMessage> sanitized = ToolArgumentSanitizer.sanitizeResponse(response, List.of());

        assertThat(sanitized).isSameAs(response);
    }

    @Test
    void shouldWrapSingleScalarArgumentWithRequiredTabName() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("tool-call-1")
                .name("getBookingList")
                .arguments("UPCOMING")
                .build();
        Response<AiMessage> response = Response.from(
                AiMessage.from(List.of(request)),
                null,
                FinishReason.TOOL_EXECUTION,
                Map.of()
        );

        Response<AiMessage> sanitized = ToolArgumentSanitizer.sanitizeResponse(
                response,
                List.of(ToolSpecification.builder()
                        .name("getBookingList")
                        .description("Booking list tool")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("tab")
                                .required("tab")
                                .build())
                        .build())
        );

        assertThat(sanitized.content().toolExecutionRequests())
                .extracting(ToolExecutionRequest::arguments)
                .containsExactly("{\"tab\":\"UPCOMING\"}");
    }

    @Test
    void shouldPreserveStructuredArgumentsForGetBookingList() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("tool-call-2")
                .name("getBookingList")
                .arguments("{\"tab\":\"HISTORY\",\"status\":\"COMPLETED\"}")
                .build();
        Response<AiMessage> response = Response.from(
                AiMessage.from(List.of(request)),
                null,
                FinishReason.TOOL_EXECUTION,
                Map.of()
        );

        Response<AiMessage> sanitized = ToolArgumentSanitizer.sanitizeResponse(
                response,
                List.of(ToolSpecification.builder()
                        .name("getBookingList")
                        .description("Booking list tool")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("tab")
                                .addStringProperty("status")
                                .required("tab")
                                .build())
                        .build())
        );

        assertThat(sanitized.content().toolExecutionRequests())
                .extracting(ToolExecutionRequest::arguments)
                .containsExactly("{\"tab\":\"HISTORY\",\"status\":\"COMPLETED\"}");
    }

    @Test
    void shouldSanitizeStoredAssistantToolMessagesBeforeReuse() {
        ChatMessage assistantMessage = AiMessage.from(List.of(ToolExecutionRequest.builder()
                .id("tool-call-3")
                .name("getBookingList")
                .arguments("tab=UPCOMING")
                .build()));

        List<ChatMessage> sanitizedMessages = ToolArgumentSanitizer.sanitizeMessages(List.of(
                UserMessage.userMessage("What is next?"),
                assistantMessage
        ));

        assertThat(sanitizedMessages.get(1)).isInstanceOf(AiMessage.class);
        AiMessage sanitizedAssistantMessage = (AiMessage) sanitizedMessages.get(1);
        assertThat(sanitizedAssistantMessage.toolExecutionRequests())
                .extracting(ToolExecutionRequest::arguments)
                .containsExactly("{\"tab\":\"UPCOMING\"}");
    }
}
