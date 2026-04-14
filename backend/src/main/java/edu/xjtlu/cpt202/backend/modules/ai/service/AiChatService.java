package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.TokenStream;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
public interface AiChatService {

    String chat(String userMessage);

    TokenStream streamChat(String userMessage);
}
