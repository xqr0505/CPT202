package edu.xjtlu.cpt202.backend.modules.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.output.Response;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes tool-call arguments so OpenAI-compatible providers receive valid JSON objects.
 * 
 * @author QiranXiao
 * @since 2026/4/17
 */
public final class ToolArgumentSanitizer {

    private static final ObjectMapper STRICT_JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper LENIENT_JSON_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();
    private static final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile("([A-Za-z0-9_]+)\\s*[:=]\\s*([^,;]+)");

    private ToolArgumentSanitizer() {
    }

    public static List<ChatMessage> sanitizeMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        boolean changed = false;
        List<ChatMessage> sanitizedMessages = new java.util.ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            ChatMessage sanitized = sanitizeMessage(message);
            sanitizedMessages.add(sanitized);
            changed |= sanitized != message;
        }

        return changed ? List.copyOf(sanitizedMessages) : messages;
    }

    public static Response<AiMessage> sanitizeResponse(
            Response<AiMessage> response,
            List<ToolSpecification> toolSpecifications
    ) {
        if (response == null || response.content() == null) {
            return response;
        }

        AiMessage sanitizedMessage = sanitizeAiMessage(response.content(), indexToolSpecifications(toolSpecifications));
        if (sanitizedMessage == response.content()) {
            return response;
        }

        return Response.from(
                sanitizedMessage,
                response.tokenUsage(),
                response.finishReason(),
                response.metadata()
        );
    }

    private static ChatMessage sanitizeMessage(ChatMessage message) {
        if (!(message instanceof AiMessage aiMessage) || !aiMessage.hasToolExecutionRequests()) {
            return message;
        }
        return sanitizeAiMessage(aiMessage, Map.of());
    }

    private static AiMessage sanitizeAiMessage(
            AiMessage aiMessage,
            Map<String, ToolSpecification> toolSpecifications
    ) {
        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
        if (toolExecutionRequests == null || toolExecutionRequests.isEmpty()) {
            return aiMessage;
        }

        boolean changed = false;
        List<ToolExecutionRequest> sanitizedRequests = new java.util.ArrayList<>(toolExecutionRequests.size());
        for (ToolExecutionRequest request : toolExecutionRequests) {
            ToolExecutionRequest sanitizedRequest = sanitizeRequest(request, toolSpecifications.get(request.name()));
            sanitizedRequests.add(sanitizedRequest);
            changed |= sanitizedRequest != request;
        }

        if (!changed) {
            return aiMessage;
        }

        List<ToolExecutionRequest> requests = List.copyOf(sanitizedRequests);
        String text = aiMessage.text();
        if (text == null || text.isBlank()) {
            return AiMessage.from(requests);
        }
        return AiMessage.from(text, requests);
    }

    private static ToolExecutionRequest sanitizeRequest(
            ToolExecutionRequest request,
            ToolSpecification toolSpecification
    ) {
        String sanitizedArguments = sanitizeArguments(request.arguments(), toolSpecification);
        if (Objects.equals(request.arguments(), sanitizedArguments)) {
            return request;
        }

        return ToolExecutionRequest.builder()
                .id(request.id())
                .name(request.name())
                .arguments(sanitizedArguments)
                .build();
    }

    private static String sanitizeArguments(String arguments, ToolSpecification toolSpecification) {
        List<String> parameterNames = extractParameterNames(toolSpecification);

        JsonNode strictNode = parseJson(arguments, STRICT_JSON_MAPPER);
        if (strictNode != null) {
            return serializeAsArgumentObject(strictNode, parameterNames);
        }

        if (arguments == null || arguments.isBlank()) {
            return "{}";
        }

        JsonNode lenientNode = parseJson(arguments, LENIENT_JSON_MAPPER);
        if (lenientNode != null) {
            return serializeAsArgumentObject(lenientNode, parameterNames);
        }

        ObjectNode keyValueNode = tryParseKeyValueObject(arguments, parameterNames);
        if (keyValueNode != null) {
            return toJson(keyValueNode);
        }

        if (parameterNames.size() == 1) {
            ObjectNode objectNode = STRICT_JSON_MAPPER.createObjectNode();
            objectNode.set(parameterNames.get(0), coerceScalar(arguments.trim()));
            return toJson(objectNode);
        }

        ObjectNode objectNode = STRICT_JSON_MAPPER.createObjectNode();
        objectNode.set("raw", TextNode.valueOf(arguments.trim()));
        return toJson(objectNode);
    }

    private static JsonNode parseJson(String arguments, ObjectMapper objectMapper) {
        if (arguments == null || arguments.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(arguments);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private static String serializeAsArgumentObject(JsonNode jsonNode, List<String> parameterNames) {
        if (jsonNode == null || jsonNode.isNull()) {
            return "{}";
        }
        if (jsonNode.isObject()) {
            return toJson(jsonNode);
        }

        ObjectNode objectNode = STRICT_JSON_MAPPER.createObjectNode();
        if (parameterNames.size() == 1) {
            objectNode.set(parameterNames.get(0), jsonNode);
        } else {
            objectNode.set("raw", jsonNode);
        }
        return toJson(objectNode);
    }

    private static ObjectNode tryParseKeyValueObject(String arguments, List<String> parameterNames) {
        Matcher matcher = KEY_VALUE_PAIR_PATTERN.matcher(arguments);
        ObjectNode objectNode = STRICT_JSON_MAPPER.createObjectNode();
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            String key = matcher.group(1);
            if (!parameterNames.isEmpty() && !parameterNames.contains(key)) {
                continue;
            }
            objectNode.set(key, coerceScalar(matcher.group(2).trim()));
        }

        if (!matched || objectNode.isEmpty()) {
            return null;
        }

        return objectNode;
    }

    private static JsonNode coerceScalar(String rawValue) {
        if (rawValue == null) {
            return STRICT_JSON_MAPPER.nullNode();
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return TextNode.valueOf("");
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return TextNode.valueOf(trimmed.substring(1, trimmed.length() - 1));
        }
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return STRICT_JSON_MAPPER.getNodeFactory().booleanNode(Boolean.parseBoolean(trimmed));
        }
        if ("null".equalsIgnoreCase(trimmed)) {
            return STRICT_JSON_MAPPER.nullNode();
        }
        if (trimmed.matches("-?\\d+")) {
            try {
                return STRICT_JSON_MAPPER.getNodeFactory().numberNode(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                return STRICT_JSON_MAPPER.getNodeFactory().numberNode(new BigDecimal(trimmed));
            }
        }
        if (trimmed.matches("-?\\d+\\.\\d+")) {
            return STRICT_JSON_MAPPER.getNodeFactory().numberNode(new BigDecimal(trimmed));
        }
        return TextNode.valueOf(trimmed);
    }

    private static List<String> extractParameterNames(ToolSpecification toolSpecification) {
        if (toolSpecification == null) {
            return List.of();
        }

        JsonObjectSchema parameters = toolSpecification.parameters();
        if (parameters == null || parameters.properties() == null || parameters.properties().isEmpty()) {
            return List.of();
        }

        return List.copyOf(parameters.properties().keySet());
    }

    private static Map<String, ToolSpecification> indexToolSpecifications(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null || toolSpecifications.isEmpty()) {
            return Map.of();
        }

        Map<String, ToolSpecification> indexed = new LinkedHashMap<>();
        for (ToolSpecification toolSpecification : toolSpecifications) {
            indexed.put(toolSpecification.name(), toolSpecification);
        }
        return Map.copyOf(indexed);
    }

    private static String toJson(JsonNode jsonNode) {
        try {
            return STRICT_JSON_MAPPER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize sanitized tool arguments", e);
        }
    }
}
