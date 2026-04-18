package edu.xjtlu.cpt202.backend.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.model.SanitizingChatLanguageModel;
import edu.xjtlu.cpt202.backend.modules.ai.model.SanitizingStreamingChatLanguageModel;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.ai.store.RedisChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Configuration
@EnableConfigurationProperties(AiChatMemoryProperties.class)
public class LangChainConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model-name}") String modelName,
            @Value("${ai.openai.base-url:}") String baseUrl
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(AiConstant.OPENAI_API_KEY_REQUIRED_MESSAGE);
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException(AiConstant.OPENAI_MODEL_NAME_REQUIRED_MESSAGE);
        }

        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);

        if (baseUrl != null) {
            String trimmedBaseUrl = baseUrl.trim();
            if (!trimmedBaseUrl.isBlank()) {
                builder.baseUrl(trimmedBaseUrl);
            }
        }

        return new SanitizingChatLanguageModel(builder.build());
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model-name}") String modelName,
            @Value("${ai.openai.base-url:}") String baseUrl
    ) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);

        if (baseUrl != null) {
            String trimmedBaseUrl = baseUrl.trim();
            if (!trimmedBaseUrl.isBlank()) {
                builder.baseUrl(trimmedBaseUrl);
            }
        }

        return new SanitizingStreamingChatLanguageModel(builder.build());
    }

    @Bean
    public ChatMemoryStore chatMemoryStore(
            RedisTemplate<String, String> redisTemplate,
            AiChatMemoryProperties aiChatMemoryProperties
    ) {
        return new RedisChatMemoryStore(redisTemplate, aiChatMemoryProperties);
    }

    @Bean
    public Assistant assistant(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            ChatMemoryStore chatMemoryStore,
            AiChatMemoryProperties aiChatMemoryProperties,
            AiBookingSearchTool aiBookingSearchTool
    ) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .tools(aiBookingSearchTool)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(aiChatMemoryProperties.getMaxMessages())
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }
}
