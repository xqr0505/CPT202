package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rewrites natural-language user questions into multiple retrieval-friendly queries.
 *
 * @author QiranXiao
 * @since 2026/4/22
 */
@Service
public class KnowledgeQueryRewriteService {

    private final ChatLanguageModel chatLanguageModel;

    public KnowledgeQueryRewriteService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public List<String> rewrite(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return List.of();
        }

        try {
            String prompt = AiConstant.KNOWLEDGE_QUERY_REWRITE_PROMPT.formatted(userQuery.trim());
            Response<dev.langchain4j.data.message.AiMessage> response =
                    chatLanguageModel.generate(List.of(UserMessage.userMessage(prompt)));
            String raw = response == null || response.content() == null ? "" : response.content().text();
            return parseThreeQueries(raw);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    List<String> parseThreeQueries(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return List.of();
        }

        List<String> parsed = Arrays.stream(rawOutput.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (parsed.size() != 3) {
            return List.of();
        }

        return parsed;
    }
}
