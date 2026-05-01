package edu.xjtlu.cpt202.backend.modules.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiRagRewriteProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeQueryRewriteServiceTest {

    private final ChatLanguageModel chatLanguageModel = mock(ChatLanguageModel.class);

    @Test
    void shouldReturnThreeQueriesWhenModelOutputIsValid() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
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
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage("only one query")));

        List<String> rewritten = rewriteService.rewrite("refund");

        assertThat(rewritten).containsExactly("refund");
    }

    @Test
    void shouldReturnEmptyWhenInputIsBlankOrModelFails() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList())).thenThrow(new RuntimeException("model failed"));

        assertThat(rewriteService.rewrite("   ")).isEmpty();
        assertThat(rewriteService.rewrite("reschedule")).isEmpty();
    }

    @Test
    void shouldUseRuleRewriteByDefaultForFastFallback() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                ruleProperties()
        );

        List<String> rewritten = rewriteService.rewrite("改签规则");

        assertThat(rewritten).isNotEmpty();
        assertThat(rewritten.get(0)).isEqualTo("改签规则");
    }

    @Test
    void shouldReturnEmptyWhenRewriteModeIsOff() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                offProperties()
        );

        assertThat(rewriteService.rewrite("改签规则")).isEmpty();
    }

    private AiRagRewriteProperties lightModelProperties() {
        AiRagRewriteProperties properties = new AiRagRewriteProperties();
        properties.setEnabled(true);
        properties.setMode("light_model");
        properties.setMaxQueries(3);
        return properties;
    }

    private AiRagRewriteProperties ruleProperties() {
        AiRagRewriteProperties properties = new AiRagRewriteProperties();
        properties.setEnabled(true);
        properties.setMode("rule");
        properties.setMaxQueries(2);
        return properties;
    }

    private AiRagRewriteProperties offProperties() {
        AiRagRewriteProperties properties = new AiRagRewriteProperties();
        properties.setEnabled(true);
        properties.setMode("off");
        properties.setMaxQueries(2);
        return properties;
    }
}
