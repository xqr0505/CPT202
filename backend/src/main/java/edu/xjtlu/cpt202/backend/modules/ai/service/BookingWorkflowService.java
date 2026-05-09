package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.TokenStream;

public interface BookingWorkflowService {

    boolean hasActiveTask(Long userId);

    boolean shouldStartWorkflow(Long userId, String originalUserMessage);

    String handle(Long userId, String normalizedUserMessage);

    TokenStream streamHandle(Long userId, String normalizedUserMessage);
}
