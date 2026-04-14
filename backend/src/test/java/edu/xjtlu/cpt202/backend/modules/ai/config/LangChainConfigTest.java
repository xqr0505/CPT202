package edu.xjtlu.cpt202.backend.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LangChainConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LangChainConfig.class)
            .withPropertyValues(
                    "ai.openai.api-key=test-openai-key",
                    "ai.openai.model-name=gpt-4o-mini"
            );

    @Test
    void shouldLoadLangChainBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChatLanguageModel.class);
            assertThat(context).hasSingleBean(Assistant.class);
        });
    }
}
