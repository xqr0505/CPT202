package edu.xjtlu.cpt202.backend.modules.ai.model;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.util.ToolArgumentSanitizer;

import java.util.List;

/**
 * Streaming variant of the tool-argument sanitizer wrapper.
 */
public class SanitizingStreamingChatLanguageModel implements StreamingChatLanguageModel {

    private final StreamingChatLanguageModel delegate;

    public SanitizingStreamingChatLanguageModel(StreamingChatLanguageModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        delegate.generate(ToolArgumentSanitizer.sanitizeMessages(messages), handler);
    }

    @Override
    public void generate(
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications,
            StreamingResponseHandler<AiMessage> handler
    ) {
        delegate.generate(
                ToolArgumentSanitizer.sanitizeMessages(messages),
                toolSpecifications,
                new SanitizingStreamingResponseHandler(handler, toolSpecifications)
        );
    }

    @Override
    public void generate(
            List<ChatMessage> messages,
            ToolSpecification toolSpecification,
            StreamingResponseHandler<AiMessage> handler
    ) {
        delegate.generate(
                ToolArgumentSanitizer.sanitizeMessages(messages),
                toolSpecification,
                new SanitizingStreamingResponseHandler(handler, List.of(toolSpecification))
        );
    }

    private static final class SanitizingStreamingResponseHandler implements StreamingResponseHandler<AiMessage> {

        private final StreamingResponseHandler<AiMessage> delegate;
        private final List<ToolSpecification> toolSpecifications;

        private SanitizingStreamingResponseHandler(
                StreamingResponseHandler<AiMessage> delegate,
                List<ToolSpecification> toolSpecifications
        ) {
            this.delegate = delegate;
            this.toolSpecifications = toolSpecifications;
        }

        @Override
        public void onNext(String token) {
            delegate.onNext(token);
        }

        @Override
        public void onComplete(Response<AiMessage> response) {
            delegate.onComplete(ToolArgumentSanitizer.sanitizeResponse(response, toolSpecifications));
        }

        @Override
        public void onError(Throwable error) {
            delegate.onError(error);
        }
    }
}
