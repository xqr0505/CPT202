package edu.xjtlu.cpt202.backend.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.service.AiServices;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.model.SanitizingChatLanguageModel;
import edu.xjtlu.cpt202.backend.modules.ai.model.SanitizingStreamingChatLanguageModel;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.impl.LightModelAiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.impl.ParallelToolAssistant;
import edu.xjtlu.cpt202.backend.modules.ai.store.RedisChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingFormTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSubmitTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiSpecialistAvailabilityTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.KnowledgeTools;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Configuration
@EnableConfigurationProperties({
        AiModelProperties.class,
        AiChatMemoryProperties.class,
        AiToolParallelProperties.class,
        AiRagRewriteProperties.class,
        AiIntentRouterProperties.class,
        AiWorkflowProperties.class
})
public class LangChainConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model-name}") String modelName,
            @Value("${ai.openai.base-url:}") String baseUrl,
            AiModelProperties aiModelProperties
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(AiConstant.OPENAI_API_KEY_REQUIRED_MESSAGE);
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException(AiConstant.OPENAI_MODEL_NAME_REQUIRED_MESSAGE);
        }

        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(aiModelProperties.getMaxOutputTokens());

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
            @Value("${ai.openai.base-url:}") String baseUrl,
            AiModelProperties aiModelProperties
    ) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(aiModelProperties.getMaxOutputTokens());

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
            AiChatMemoryProperties aiChatMemoryProperties,
            AiChatProfiler aiChatProfiler
    ) {
        return new RedisChatMemoryStore(redisTemplate, aiChatMemoryProperties, aiChatProfiler);
    }

    @Bean
    public Assistant assistant(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            ChatMemoryStore chatMemoryStore,
            AiChatMemoryProperties aiChatMemoryProperties,
            AiToolParallelProperties aiToolParallelProperties,
            AiChatProfiler aiChatProfiler,
            AiIntentRouterService intentRouterService,
            AiSemanticCacheService semanticCacheService,
            AiBookingSearchTool aiBookingSearchTool,
            AiBookingFormTool aiBookingFormTool,
            AiBookingSubmitTool aiBookingSubmitTool,
            AiSpecialistAvailabilityTool aiSpecialistAvailabilityTool,
            ObjectProvider<KnowledgeTools> knowledgeToolsProvider
    ) {
        List<Object> tools = new ArrayList<>();
        tools.add(aiBookingSearchTool);
        tools.add(aiBookingFormTool);
        tools.add(aiBookingSubmitTool);
        tools.add(aiSpecialistAvailabilityTool);
        knowledgeToolsProvider.ifAvailable(tools::add);

        Map<AiIntent, List<Object>> groupedTools = new EnumMap<>(AiIntent.class);
        groupedTools.put(
                AiIntent.KNOWLEDGE,
                knowledgeToolsProvider.getIfAvailable() == null
                        ? List.of()
                        : List.of(knowledgeToolsProvider.getIfAvailable())
        );
        groupedTools.put(
                AiIntent.CANCEL,
                List.of()
        );
        groupedTools.put(
                AiIntent.BOOKING,
                List.of(aiSpecialistAvailabilityTool, aiBookingSubmitTool, aiBookingFormTool)
        );
        groupedTools.put(
                AiIntent.DASHBOARD,
                List.of(aiBookingSearchTool)
        );
        groupedTools.put(
                AiIntent.CHITCHAT,
                List.of()
        );

        return new ParallelToolAssistant(
                chatLanguageModel,
                streamingChatLanguageModel,
                chatMemoryStore,
                aiChatMemoryProperties,
                aiToolParallelProperties,
                aiChatProfiler,
                AiConstant.AI_SYSTEM_PROMPT,
                tools,
                intentRouterService,
                groupedTools,
                semanticCacheService
        );
    }

    @Bean
    public CancelWorkflowAssistant cancelWorkflowAssistant(ChatLanguageModel chatLanguageModel) {
        return AiServices.builder(CancelWorkflowAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    @Bean
    public AiIntentRouterService intentRouterService(
            ChatLanguageModel chatLanguageModel,
            AiIntentRouterProperties aiIntentRouterProperties
    ) {
        return new LightModelAiIntentRouterService(chatLanguageModel, aiIntentRouterProperties);
    }
}
