package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiRagRewriteProperties;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    private final AiRagRewriteProperties rewriteProperties;

    public KnowledgeQueryRewriteService(
            ChatLanguageModel chatLanguageModel,
            AiRagRewriteProperties rewriteProperties
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.rewriteProperties = rewriteProperties;
    }

    public List<String> rewrite(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return List.of();
        }

        String normalizedQuery = userQuery.trim();
        if (!rewriteProperties.isEnabled() || rewriteProperties.modeEnum() == AiRagRewriteProperties.RewriteMode.OFF) {
            return List.of();
        }

        if (rewriteProperties.modeEnum() == AiRagRewriteProperties.RewriteMode.RULE) {
            return ruleRewrite(normalizedQuery);
        }

        try {
            String prompt = AiConstant.KNOWLEDGE_QUERY_REWRITE_PROMPT.formatted(normalizedQuery);
            Response<dev.langchain4j.data.message.AiMessage> response =
                    chatLanguageModel.generate(List.of(UserMessage.userMessage(prompt)));
            String raw = response == null || response.content() == null ? "" : response.content().text();
            return limitQueries(parseThreeQueries(raw), normalizedQuery);
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

    private List<String> ruleRewrite(String userQuery) {
        String normalized = userQuery.toLowerCase(Locale.ROOT);
        List<String> candidates = List.of(
                userQuery,
                normalized
                        .replace("改签", "reschedule")
                        .replace("规则", "policy")
                        .replace("预约", "booking"),
                normalized
                        .replace("改期", "reschedule")
                        .replace("变更", "change")
        ).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
        return limitQueries(candidates, userQuery);
    }

    private List<String> limitQueries(List<String> queries, String fallbackQuery) {
        List<String> deduplicated = queries.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(Math.max(1, rewriteProperties.getMaxQueries()))
                .collect(Collectors.toList());
        if (deduplicated.isEmpty()) {
            return List.of(fallbackQuery);
        }
        return deduplicated;
    }
}
