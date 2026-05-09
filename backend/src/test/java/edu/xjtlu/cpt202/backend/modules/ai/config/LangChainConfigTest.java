package edu.xjtlu.cpt202.backend.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingFormTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSubmitTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiSpecialistSearchTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiSpecialistAvailabilityTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.KnowledgeTools;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author QiranXiao
 * @since 2026/4/15
 *
 */
class LangChainConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LangChainConfig.class)
            .withBean("redisTemplate", RedisTemplate.class, () -> {
                RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
                redisTemplate.setConnectionFactory(mock(RedisConnectionFactory.class));
                return redisTemplate;
            })
            .withBean(AiBookingSearchTool.class, () -> mock(AiBookingSearchTool.class))
            .withBean(AiBookingFormTool.class, () -> mock(AiBookingFormTool.class))
            .withBean(AiBookingSubmitTool.class, () -> mock(AiBookingSubmitTool.class))
            .withBean(AiSpecialistSearchTool.class, () -> mock(AiSpecialistSearchTool.class))
            .withBean(AiSpecialistAvailabilityTool.class, () -> mock(AiSpecialistAvailabilityTool.class))
            .withBean(AiSemanticCacheService.class, () -> mock(AiSemanticCacheService.class))
            .withBean(KnowledgeTools.class, () -> mock(KnowledgeTools.class))
            .withBean(CommonProperties.class, CommonProperties::new)
            .withBean(AiChatProfiler.class, () -> new AiChatProfiler(new CommonProperties()))
            .withPropertyValues(
                    "ai.openai.api-key=test-openai-key",
                    "ai.openai.model-name=gpt-4o-mini"
            );

    @Test
    void shouldLoadLangChainBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("chatLanguageModel");
            assertThat(context).hasBean("intentRouterChatLanguageModel");
            assertThat(context).hasSingleBean(Assistant.class);
            assertThat(context).hasSingleBean(ChatMemoryStore.class);
            assertThat(context.getBean(AiChatMemoryProperties.class).getMaxMessages()).isEqualTo(20);
            assertThat(context.getBean(AiChatMemoryProperties.class).getTtlSeconds()).isEqualTo(86400L);
            assertThat(context.getBean(AiChatMemoryProperties.class).getKeyPrefix())
                    .isEqualTo("expertlink:ai:memory");
        });
    }

    @Test
    void shouldOverrideChatMemoryProperties() {
        contextRunner
                .withPropertyValues(
                        "ai.chat.memory.max-messages=8",
                        "ai.chat.memory.ttl-seconds=120",
                        "ai.chat.memory.key-prefix=test:ai:memory"
                )
                .run(context -> {
                    AiChatMemoryProperties properties = context.getBean(AiChatMemoryProperties.class);
                    assertThat(properties.getMaxMessages()).isEqualTo(8);
                    assertThat(properties.getTtlSeconds()).isEqualTo(120L);
                    assertThat(properties.getKeyPrefix()).isEqualTo("test:ai:memory");
                });
    }

    @Test
    void shouldOverrideModelProperties() {
        contextRunner
                .withPropertyValues("ai.model.max-output-tokens=256")
                .run(context -> {
                    AiModelProperties properties = context.getBean(AiModelProperties.class);
                    assertThat(properties.getMaxOutputTokens()).isEqualTo(256);
                });
    }
}
