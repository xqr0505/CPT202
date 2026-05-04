package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.TokenStream;

/**
 * Dedicated booking workflow entrypoint.
 *
 * @author QiranXiao
 * @since 2026/5/5
 */
public interface BookingWorkflowService {

    boolean hasActiveTask(Long userId);

    boolean shouldStartWorkflow(Long userId, String originalUserMessage);

    String handle(Long userId, String normalizedUserMessage);

    TokenStream streamHandle(Long userId, String normalizedUserMessage);
}
