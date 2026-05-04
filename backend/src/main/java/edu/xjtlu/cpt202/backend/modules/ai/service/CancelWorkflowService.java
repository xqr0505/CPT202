package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.TokenStream;

/**
 * Dedicated cancellation workflow entrypoint.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
public interface CancelWorkflowService {

    boolean hasActiveTask(Long userId);

    boolean shouldStartWorkflow(Long userId, String originalUserMessage);

    String handle(Long userId, String normalizedUserMessage);

    TokenStream streamHandle(Long userId, String normalizedUserMessage);
}
