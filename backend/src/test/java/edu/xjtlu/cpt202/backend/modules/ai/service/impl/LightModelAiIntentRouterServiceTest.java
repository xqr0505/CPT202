package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiIntentRouterProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @since 2026/5/5
 */
class LightModelAiIntentRouterServiceTest {

    @Test
    void shouldRouteExplicitBookingOrderToBookingIntent() {
        ChatLanguageModel lightModel = mock(ChatLanguageModel.class);
        LightModelAiIntentRouterService service = new LightModelAiIntentRouterService(
                lightModel,
                properties()
        );

        AiIntent intent = service.resolveIntent(1001L, "please place booking order now");

        assertThat(intent).isEqualTo(AiIntent.BOOKING);
        verifyNoInteractions(lightModel);
    }

    @Test
    void shouldNotRouteGenericAvailabilityQueryToBookingIntent() {
        ChatLanguageModel lightModel = mock(ChatLanguageModel.class);
        when(lightModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("KNOWLEDGE")));
        LightModelAiIntentRouterService service = new LightModelAiIntentRouterService(
                lightModel,
                properties()
        );

        AiIntent intent = service.resolveIntent(1001L, "show me available specialists tomorrow at 10");

        assertThat(intent).isEqualTo(AiIntent.KNOWLEDGE);
        verify(lightModel).generate(anyList());
    }

    private static AiIntentRouterProperties properties() {
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        properties.setTimeoutMs(800L);
        return properties;
    }
}
