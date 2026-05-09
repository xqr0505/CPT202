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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeQueryRewriteServiceTest {

    private final ChatLanguageModel chatLanguageModel = mock(ChatLanguageModel.class);

    @Test
    void shouldReturnParsedQueriesWhenModelOutputIsValid() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage(
                        "specialist search empty, reset search filters, cannot find specialist profile"
                )));

        List<String> rewritten = rewriteService.rewrite(
                "I cannot find specialists after applying multiple filters in the booking search page"
        );

        assertThat(rewritten).containsExactly(
                "specialist search empty",
                "reset search filters"
        );
    }

    @Test
    void shouldParseMessyModelOutputWithNumberingAndPrefix() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage(
                        "Here are the queries: 1. refund policy after cancellation; 2) booking reschedule rules\n3: cancellation fee conditions"
                )));

        List<String> rewritten = rewriteService.rewrite(
                "Please explain refund and cancellation fee policy when I cancel too close to appointment time"
        );

        assertThat(rewritten).containsExactly(
                "refund policy after cancellation",
                "booking reschedule rules"
        );
    }

    @Test
    void shouldReturnAvailableQueriesWhenModelOutputsOneOrTwo() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.aiMessage("reschedule approval rules, slot change policy")));

        List<String> rewritten = rewriteService.rewrite(
                "I want to change booking time and check whether approval is required again for specialist"
        );

        assertThat(rewritten).containsExactly("reschedule approval rules", "slot change policy");
    }

    @Test
    void shouldSkipRewriteForShortQueryAndNotCallModel() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );

        List<String> rewritten = rewriteService.rewrite("refund policy");

        assertThat(rewritten).isEmpty();
        verify(chatLanguageModel, never()).generate(anyList());
    }

    @Test
    void shouldReturnEmptyWhenInputIsBlankOrModelFails() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                lightModelProperties()
        );
        when(chatLanguageModel.generate(anyList())).thenThrow(new RuntimeException("model failed"));

        assertThat(rewriteService.rewrite("   ")).isEmpty();
        assertThat(rewriteService.rewrite(
                "Please explain how cancellation policy differs between pending and confirmed bookings today"
        )).isEmpty();
    }

    @Test
    void shouldUseRuleRewriteByDefaultForFastFallbackWhenLongEnough() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                ruleProperties()
        );

        List<String> rewritten = rewriteService.rewrite(
                "reschedule policy booking change process for cancelled appointment and user operation guide"
        );

        assertThat(rewritten).isNotEmpty();
        assertThat(rewritten.get(0)).isEqualTo(
                "reschedule policy booking change process for cancelled appointment and user operation guide"
        );
    }

    @Test
    void shouldReturnEmptyWhenRewriteModeIsOff() {
        KnowledgeQueryRewriteService rewriteService = new KnowledgeQueryRewriteService(
                chatLanguageModel,
                offProperties()
        );

        assertThat(rewriteService.rewrite(
                "reschedule policy booking change process for cancelled appointment and user operation guide"
        )).isEmpty();
    }

    private AiRagRewriteProperties lightModelProperties() {
        AiRagRewriteProperties properties = new AiRagRewriteProperties();
        properties.setEnabled(true);
        properties.setMode("light_model");
        properties.setMaxQueries(2);
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
