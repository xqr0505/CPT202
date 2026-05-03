package edu.xjtlu.cpt202.backend.modules.ai.service;

/**
 * Routes user messages to intent labels for dynamic tool selection.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public interface AiIntentRouterService {

    AiIntent resolveIntent(Long memoryId, String userMessage);
}
