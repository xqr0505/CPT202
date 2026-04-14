package edu.xjtlu.cpt202.backend.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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

        return builder.build();
    }

    @Bean
    public Assistant assistant(ChatLanguageModel chatLanguageModel) {
        return AiServices.create(Assistant.class, chatLanguageModel);
    }
}
