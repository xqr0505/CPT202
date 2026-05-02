package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiIntentRouterProperties;
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
import java.util.regex.Pattern;

/**
 * Fast intent router based on rules-first matching with lightweight model fallback.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public class LightModelAiIntentRouterService implements AiIntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(LightModelAiIntentRouterService.class);
    private static final Set<String> ALLOWED = Set.of("KNOWLEDGE", "BOOKING", "DASHBOARD", "CHITCHAT");
    private static final String ROUTER_PROMPT = """
        You are an intent router for ExpertLink. 
        Classify the user message into exactly ONE label:

        KNOWLEDGE
        BOOKING
        DASHBOARD
        CHITCHAT

        Definitions:
        - BOOKING: user wants to do appointment workflow actions, such as:
        booking a specialist, checking a specialist's availability/time slots, rescheduling, canceling, submitting booking details.
        - DASHBOARD: user wants to view their own booking records/history/status/statistics/upcoming or past appointments.
        - KNOWLEDGE: platform policy/rules/how-to questions, e.g. refund/cancellation policy, booking status meaning, platform usage guidance.
        - CHITCHAT: pure small talk only (greeting/thanks/self-introduction) with no product task.

        Priority rules:
        1) If message asks for own records/history/status list/overview -> DASHBOARD.
        2) Else if message asks to book/reschedule/cancel/check specialist availability/time -> BOOKING.
        3) Else if message asks policy/rules/platform usage/meaning/explanation -> KNOWLEDGE.
        4) If mixed or unclear, choose KNOWLEDGE by default.
        5) Do NOT choose CHITCHAT unless it is pure small talk and contains no product request.

        Output rules:
        - Output exactly one word from: KNOWLEDGE, BOOKING, DASHBOARD, CHITCHAT
        - No extra text.

        User message:
        %s

        """;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> CHITCHAT_EXACT = Set.of(
            "hello", "hi", "hey", "thanks", "thank you",
            "good morning", "good afternoon", "good evening",
            "who are you", "what can you do", "how are you"
    );
    private static final Set<String> KNOWLEDGE_HINTS = Set.of(
            "policy", "policies", "rule", "rules", "meaning", "mean", "what does",
            "how to", "why", "guide", "help", "refund", "cancellation", "cancelation",
            "status mean", "what is", "what are", "explain", "platform",
            "what should i do", "unsupported characters", "error", "failed",
            "confirmed automatically", "will it", "if i ", "should i"
    );
    private static final Set<String> DASHBOARD_HINTS = Set.of(
            "my bookings", "my booking", "booking records", "booking history",
            "upcoming bookings", "past bookings", "dashboard", "statistics", "stats"
    );
    private static final Set<String> BOOKING_ACTION_HINTS = Set.of(
            "i want to book", "book for me", "book a specialist", "book specialist",
            "check availability", "specialist availability", "available time", "time slot",
            "reschedule my booking", "cancel my booking", "submit booking",
            "schedule an appointment", "make a booking"
    );

    private final ChatLanguageModel lightModel;
    private final AiIntentRouterProperties properties;

    public LightModelAiIntentRouterService(
            ChatLanguageModel lightModel,
            AiIntentRouterProperties properties
    ) {
        this.lightModel = lightModel;
        this.properties = properties;
    }

    @Override
    public AiIntent resolveIntent(Long memoryId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return AiIntent.KNOWLEDGE;
        }

        String normalizedMessage = normalize(userMessage);
        AiIntent ruleBasedIntent = resolveByRules(normalizedMessage);
        if (ruleBasedIntent != null) {
            return ruleBasedIntent;
        }

        return resolveByModel(normalizedMessage);
    }

    private AiIntent resolveByModel(String normalizedMessage) {
        try {
            CompletableFuture<Response<AiMessage>> future = CompletableFuture.supplyAsync(() ->
                    lightModel.generate(List.of(UserMessage.userMessage(ROUTER_PROMPT.formatted(normalizedMessage))))
            );
            Response<AiMessage> response = future.get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            String raw = response == null || response.content() == null ? "" : response.content().text();
            String parsed = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            if (ALLOWED.contains(parsed)) {
                return AiIntent.valueOf(parsed);
            }
            log.debug("Intent router fallback due to unknown output: {}", raw);
            return AiIntent.KNOWLEDGE;
        } catch (TimeoutException timeoutException) {
            log.debug("Intent router timeout after {} ms", properties.getTimeoutMs());
            return AiIntent.KNOWLEDGE;
        } catch (RuntimeException exception) {
            log.debug("Intent router fallback due to error: {}", exception.getMessage());
            return AiIntent.KNOWLEDGE;
        } catch (Exception exception) {
            log.debug("Intent router fallback due to checked error: {}", exception.getMessage());
            return AiIntent.KNOWLEDGE;
        }
    }

    private AiIntent resolveByRules(String normalizedMessage) {
        if (CHITCHAT_EXACT.contains(normalizedMessage)) {
            return AiIntent.CHITCHAT;
        }
        if (containsAny(normalizedMessage, KNOWLEDGE_HINTS.toArray(String[]::new))) {
            return AiIntent.KNOWLEDGE;
        }
        if (containsAny(normalizedMessage, DASHBOARD_HINTS.toArray(String[]::new))) {
            return AiIntent.DASHBOARD;
        }
        if (containsAny(normalizedMessage, BOOKING_ACTION_HINTS.toArray(String[]::new))) {
            return AiIntent.BOOKING;
        }
        return null;
    }

    private String normalize(String userMessage) {
        return WHITESPACE.matcher(userMessage.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    private boolean containsAny(String message, String... phrases) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
}
