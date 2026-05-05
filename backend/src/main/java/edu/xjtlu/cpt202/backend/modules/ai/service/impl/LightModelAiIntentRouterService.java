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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Fast intent router based on rules-first matching with lightweight model fallback.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public class LightModelAiIntentRouterService implements AiIntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(LightModelAiIntentRouterService.class);
    private static final ExecutorService MODEL_EXECUTOR = Executors.newCachedThreadPool(new IntentRouterThreadFactory());
    private static final Set<String> ALLOWED = Set.of("KNOWLEDGE", "CANCEL", "BOOKING", "DASHBOARD", "CHITCHAT");
    private static final String ROUTER_PROMPT = """
        You are an intent router for ExpertLink. 
        Classify the user message into exactly ONE label:

        KNOWLEDGE
        CANCEL
        BOOKING
        DASHBOARD
        CHITCHAT

        Definitions:
        - CANCEL: user wants to cancel an existing booking or continue an in-progress cancellation flow.
        - RESCHEDULE: user wants to reschedule OR change the time of an existing booking or continue an in-progress reschedule flow.
        - BOOKING: user explicitly wants to place/submit a booking order now.
        - DASHBOARD: user wants to view their own booking records/history/status/statistics/upcoming or past appointments.
        - KNOWLEDGE: platform policy/rules/how-to questions, e.g. refund/cancellation policy, booking status meaning, platform usage guidance.
        - CHITCHAT: pure small talk only (greeting/thanks/self-introduction) with no product task.

        Priority rules:
        1) If message asks for own records/history/status list/overview -> DASHBOARD.
        2) Else if message asks to cancel an existing booking -> CANCEL.
        3) Else if message clearly asks to place/submit a booking order now -> BOOKING.
        4) Else if message asks policy/rules/platform usage/meaning/explanation -> KNOWLEDGE.
        5) If mixed or unclear, choose KNOWLEDGE by default.
        6) Do NOT choose CHITCHAT unless it is pure small talk and contains no product request.

        Output rules:
        - Output exactly one word from: KNOWLEDGE, CANCEL, BOOKING, DASHBOARD, CHITCHAT
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
            "how to", "why", "guide", "refund", "cancellation", "cancelation",
            "status mean", "what is", "what are", "explain", "platform",
            "what should i do", "unsupported characters", "error", "failed",
            "confirmed automatically", "will it", "if i ", "should i"
    );
    private static final Set<String> DASHBOARD_HINTS = Set.of(
            "check my bookings", "booking records", "booking history",
            "upcoming bookings", "past bookings", "dashboard", "statistics", "stats"
    );
    private static final Set<String> BOOKING_ACTION_HINTS = Set.of(
            "i want to book", "book now", "book it now", "book for me",
            "place booking", "place a booking", "place order",
            "submit booking", "confirm booking", "book this slot",
            "下单", "我要预约", "帮我预约", "帮我下单", "提交预约", "确认预约"
    );
    private static final Set<String> CANCEL_ACTION_HINTS = Set.of(
            "cancel my booking", "cancel booking", "cancel appointment",
            "cancel my appointment", "i want to cancel", "help me cancel"
    );
    private static final Set<String> RESCHEDULE_ACTION_HINTS = Set.of(
            "reschedule", "reschedul", "rescheduling",
            "reschedule my booking", "reschedule booking", "reschedule appointment",
            "change time", "change my booking", "change booking", "change appointment",
            "move my booking", "move booking", "move appointment"
    );

    private final ChatLanguageModel lightModel;
    private final AiIntentRouterProperties properties;
    private final ExecutorService modelExecutor;

    public LightModelAiIntentRouterService(
            ChatLanguageModel lightModel,
            AiIntentRouterProperties properties
    ) {
        this(lightModel, properties, MODEL_EXECUTOR);
    }

    LightModelAiIntentRouterService(
            ChatLanguageModel lightModel,
            AiIntentRouterProperties properties,
            ExecutorService modelExecutor
    ) {
        this.lightModel = lightModel;
        this.properties = properties;
        this.modelExecutor = modelExecutor;
    }

    @Override
    public AiIntent resolveIntent(Long memoryId, String userMessage) {
        return resolveIntentDecision(memoryId, userMessage).intent();
    }

    @Override
    public IntentDecision resolveIntentDecision(Long memoryId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.NONE);
        }

        String normalizedMessage = normalize(userMessage);
        AiIntent ruleBasedIntent = resolveByRules(normalizedMessage);
        if (ruleBasedIntent != null) {
            return new IntentDecision(ruleBasedIntent, FallbackReason.NONE);
        }

        return resolveByModel(normalizedMessage);
    }

    private IntentDecision resolveByModel(String normalizedMessage) {
        Future<Response<AiMessage>> future = null;
        try {
            future = modelExecutor.submit(() ->
                    lightModel.generate(List.of(UserMessage.userMessage(ROUTER_PROMPT.formatted(normalizedMessage))))
            );
            Response<AiMessage> response = future.get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            String raw = response == null || response.content() == null ? "" : response.content().text();
            String parsed = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            if (ALLOWED.contains(parsed)) {
                return new IntentDecision(AiIntent.valueOf(parsed), FallbackReason.NONE);
            }
            log.debug("Intent router fallback due to unknown output: {}", raw);
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.ERROR_FALLBACK);
        } catch (TimeoutException timeoutException) {
            cancelQuietly(future);
            log.debug("Intent router model call timed out after {} ms; falling back to KNOWLEDGE", properties.getTimeoutMs());
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.TIMEOUT_FALLBACK);
        } catch (InterruptedException interruptedException) {
            cancelQuietly(future);
            Thread.currentThread().interrupt();
            log.debug("Intent router interrupted; falling back to KNOWLEDGE");
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.ERROR_FALLBACK);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause() == null ? executionException : executionException.getCause();
            log.debug("Intent router fallback due to model error: {}", cause.getMessage());
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.ERROR_FALLBACK);
        } catch (RuntimeException exception) {
            log.debug("Intent router fallback due to error: {}", exception.getMessage());
            return new IntentDecision(AiIntent.KNOWLEDGE, FallbackReason.ERROR_FALLBACK);
        }
    }

    private void cancelQuietly(Future<Response<AiMessage>> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private AiIntent resolveByRules(String normalizedMessage) {
        if (CHITCHAT_EXACT.contains(normalizedMessage)) {
            return AiIntent.CHITCHAT;
        }
        if (containsAny(normalizedMessage, CANCEL_ACTION_HINTS.toArray(String[]::new))) {
            return AiIntent.CANCEL;
        }
        if (containsAny(normalizedMessage, RESCHEDULE_ACTION_HINTS.toArray(String[]::new))) {
            // Keep router output compatible with existing intent enum.
            // Reschedule workflow start is decided by workflow-level trigger matching.
            return AiIntent.KNOWLEDGE;
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

    private static final class IntentRouterThreadFactory implements ThreadFactory {

        private static final AtomicInteger COUNTER = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ai-intent-router-" + COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
