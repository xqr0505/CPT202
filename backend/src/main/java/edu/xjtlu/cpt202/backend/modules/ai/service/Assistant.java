package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
public interface Assistant {

    @SystemMessage(AiConstant.AI_SYSTEM_PROMPT)
    String chat(@UserMessage String userMessage);

    @SystemMessage(AiConstant.AI_SYSTEM_PROMPT)
    TokenStream streamChat(@UserMessage String userMessage);
}
