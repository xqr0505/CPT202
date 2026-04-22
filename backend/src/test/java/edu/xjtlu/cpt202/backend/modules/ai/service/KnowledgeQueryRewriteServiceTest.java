package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeQueryRewriteServiceTest {

    private final ChatLanguageModel chatLanguageModel = mock(ChatLanguageModel.class);
    private final KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(chatLanguageModel);

    @Test
    void shouldReturnThreeQueriesWhenModelOutputIsValid() {
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage("specialist search empty, reset search filters, cannot find specialist profile")));

        List<String> rewritten = rewriteService.rewrite("I can't find the doctor");

        assertThat(rewritten).containsExactly(
                "specialist search empty",
                "reset search filters",
                "cannot find specialist profile"
        );
    }

    @Test
    void shouldReturnEmptyWhenModelOutputIsInvalidCount() {
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage("only one query")));

        List<String> rewritten = rewriteService.rewrite("refund");

        assertThat(rewritten).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenInputIsBlankOrModelFails() {
        when(chatLanguageModel.generate(anyList())).thenThrow(new RuntimeException("model failed"));

        assertThat(rewriteService.rewrite("   ")).isEmpty();
        assertThat(rewriteService.rewrite("reschedule")).isEmpty();
    }
}
