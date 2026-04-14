package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.service.TokenStream;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author QiranXiao
 * @since 2026/4/15
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private final Assistant assistant;

    public AiChatServiceImpl(Assistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public String chat(String userMessage) {
        return assistant.chat(Optional.ofNullable(userMessage).orElse(""));
    }

    @Override
    public TokenStream streamChat(String userMessage) {
        return assistant.streamChat(Optional.ofNullable(userMessage).orElse(""));
    }
}
