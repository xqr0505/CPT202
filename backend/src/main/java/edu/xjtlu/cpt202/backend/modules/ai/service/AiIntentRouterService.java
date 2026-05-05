package edu.xjtlu.cpt202.backend.modules.ai.service;

/**
 * Routes user messages to intent labels for dynamic tool selection.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public interface AiIntentRouterService {

    AiIntent resolveIntent(Long memoryId, String userMessage);

    default IntentDecision resolveIntentDecision(Long memoryId, String userMessage) {
        AiIntent intent = resolveIntent(memoryId, userMessage);
        return new IntentDecision(intent, FallbackReason.NONE);
    }

    record IntentDecision(AiIntent intent, FallbackReason fallbackReason) {
        public boolean isKnowledgeTimeoutFallback() {
            return intent == AiIntent.KNOWLEDGE && fallbackReason == FallbackReason.TIMEOUT_FALLBACK;
        }
    }

    enum FallbackReason {
        NONE,
        TIMEOUT_FALLBACK,
        ERROR_FALLBACK
    }
}
