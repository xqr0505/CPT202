package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fast intent router based on lightweight model output.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public class LightModelAiIntentRouterService implements AiIntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(LightModelAiIntentRouterService.class);
    private static final Set<String> ALLOWED = Set.of("KNOWLEDGE", "BOOKING", "DASHBOARD", "CHITCHAT");
    private static final long ROUTER_TIMEOUT_MS = 1200L;
    private static final String ROUTER_PROMPT = """
            Classify user intent for ExpertLink support into exactly one word:
            KNOWLEDGE, BOOKING, DASHBOARD, or CHITCHAT.
            Return exactly one word and nothing else.
            User: %s
            """;

    private final ChatLanguageModel lightModel;

    public LightModelAiIntentRouterService(ChatLanguageModel lightModel) {
        this.lightModel = lightModel;
    }

    @Override
    public AiIntent resolveIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return AiIntent.KNOWLEDGE;
        }
        try {
            CompletableFuture<Response<AiMessage>> future = CompletableFuture.supplyAsync(() ->
                    lightModel.generate(List.of(UserMessage.userMessage(ROUTER_PROMPT.formatted(userMessage.trim()))))
            );
            Response<AiMessage> response = future.get(ROUTER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            String raw = response == null || response.content() == null ? "" : response.content().text();
            String parsed = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            if (ALLOWED.contains(parsed)) {
                return AiIntent.valueOf(parsed);
            }
            log.debug("Intent router fallback due to unknown output: {}", raw);
            return AiIntent.KNOWLEDGE;
        } catch (TimeoutException timeoutException) {
            log.debug("Intent router timeout after {} ms", ROUTER_TIMEOUT_MS);
            return AiIntent.KNOWLEDGE;
        } catch (RuntimeException exception) {
            log.debug("Intent router fallback due to error: {}", exception.getMessage());
            return AiIntent.KNOWLEDGE;
        } catch (Exception exception) {
            log.debug("Intent router fallback due to checked error: {}", exception.getMessage());
            return AiIntent.KNOWLEDGE;
        }
    }
}
