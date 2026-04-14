package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI assistant service interface.
 */
@SystemMessage("You are a concise and helpful assistant for ExpertLink users.")
public interface Assistant {

    String chat(@UserMessage String userMessage);
}
