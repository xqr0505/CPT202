package edu.xjtlu.cpt202.backend.modules.ai.model;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.util.ToolArgumentSanitizer;

import java.util.List;
import java.util.Set;

/**
 * Wraps a chat model and ensures tool-call arguments stay valid JSON objects.
 */
public class SanitizingChatLanguageModel implements ChatLanguageModel {

    private final ChatLanguageModel delegate;

    public SanitizingChatLanguageModel(ChatLanguageModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return delegate.generate(ToolArgumentSanitizer.sanitizeMessages(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        Response<AiMessage> response = delegate.generate(
                ToolArgumentSanitizer.sanitizeMessages(messages),
                toolSpecifications
        );
        return ToolArgumentSanitizer.sanitizeResponse(response, toolSpecifications);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        Response<AiMessage> response = delegate.generate(
                ToolArgumentSanitizer.sanitizeMessages(messages),
                toolSpecification
        );
        return ToolArgumentSanitizer.sanitizeResponse(response, List.of(toolSpecification));
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return delegate.supportedCapabilities();
    }
}
