package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.config.RedisConfig;
import edu.xjtlu.cpt202.backend.modules.ai.model.RagResultItem;
import edu.xjtlu.cpt202.backend.modules.ai.rag.MarkdownHeadingSegmenter;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagConfig;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagIngestionService;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.ai.service.KnowledgeQueryRewriteService;
import edu.xjtlu.cpt202.backend.modules.ai.tool.KnowledgeTools;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingFormTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSubmitTool;
import edu.xjtlu.cpt202.backend.modules.ai.config.LangChainConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real RAG export runner.
 * Requires valid runtime dependencies:
 * 1) Redis running and reachable
 * 2) OPENAI_API_KEY + OPENAI_MODEL_NAME
 * 3) DASHSCOPE_API_KEY
 */
@SpringBootTest(
        classes = AiRagasExportIntegrationTest.TestApplication.class,
        properties = {
                "ai.tools.parallel.enabled=false",
                "ai.tools.parallel.timeout-ms=60000"
        }
)
@ActiveProfiles("dev")
@Disabled("External RAG export runner requires Redis/OpenAI/DashScope and is excluded from unit test mvn test run.")
class AiRagasExportIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Path DATASET_PATH = Path.of("src", "test", "resources", "ragas", "faq_eval_dataset.json");
    private static final Path RESULT_JSONL_PATH = Path.of("results.json");
    private static final Path RAGAS_DATA_PATH = Path.of("ragas_data.json");

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private KnowledgeTools knowledgeTools;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldCallRealRagAndExportRagasDataShape() throws IOException {
        List<FaqEvalSample> samples = OBJECT_MAPPER.readValue(DATASET_PATH.toFile(), new TypeReference<>() {
        });
        assertThat(samples).hasSize(24);

        if (Files.exists(RESULT_JSONL_PATH)) {
            Files.delete(RESULT_JSONL_PATH);
        }
        if (Files.exists(RAGAS_DATA_PATH)) {
            Files.delete(RAGAS_DATA_PATH);
        }

        List<String> questions = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        List<List<String>> contexts = new ArrayList<>();
        List<String> groundTruths = new ArrayList<>();

        UserContextHolder.setUserId(1001L);

        try (BufferedWriter writer = Files.newBufferedWriter(
                RESULT_JSONL_PATH,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            for (FaqEvalSample sample : samples) {
                String question = sample.getQuestion();
                String answer = aiChatService.chat(question);
                String context = knowledgeTools.searchKnowledgeBase(question);

                RagResultItem item = new RagResultItem(question, answer, context, sample.getGroundTruth());
                writer.write(OBJECT_MAPPER.writeValueAsString(item));
                writer.newLine();

                questions.add(question);
                answers.add(answer);
                contexts.add(List.of(context));
                groundTruths.add(sample.getGroundTruth());
            }
        }

        Map<String, Object> ragasData = new LinkedHashMap<>();
        ragasData.put("question", questions);
        ragasData.put("answer", answers);
        ragasData.put("contexts", contexts);
        ragasData.put("ground_truth", groundTruths);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(RAGAS_DATA_PATH.toFile(), ragasData);

        List<String> lines = Files.readAllLines(RESULT_JSONL_PATH, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(samples.size());
        for (String line : lines) {
            JsonNode node = OBJECT_MAPPER.readTree(line);
            assertThat(node.has("question")).isTrue();
            assertThat(node.has("answer")).isTrue();
            assertThat(node.has("context")).isTrue();
            assertThat(node.has("groundTruth")).isTrue();
        }

        JsonNode ragasNode = OBJECT_MAPPER.readTree(RAGAS_DATA_PATH.toFile());
        assertThat(ragasNode.has("question")).isTrue();
        assertThat(ragasNode.has("answer")).isTrue();
        assertThat(ragasNode.has("contexts")).isTrue();
        assertThat(ragasNode.has("ground_truth")).isTrue();
        assertThat(ragasNode.get("question").size()).isEqualTo(samples.size());
        assertThat(ragasNode.get("answer").size()).isEqualTo(samples.size());
        assertThat(ragasNode.get("contexts").size()).isEqualTo(samples.size());
        assertThat(ragasNode.get("ground_truth").size()).isEqualTo(samples.size());
    }

    private static final class FaqEvalSample {
        private String category;
        private String question;
        private String groundTruth;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getGroundTruth() {
            return groundTruth;
        }

        public void setGroundTruth(String groundTruth) {
            this.groundTruth = groundTruth;
        }
    }

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            MybatisPlusAutoConfiguration.class
    })
    @Import({
            RedisConfig.class,
            LangChainConfig.class,
            RagConfig.class,
            RagIngestionService.class,
            MarkdownHeadingSegmenter.class,
            KnowledgeQueryRewriteService.class,
            KnowledgeTools.class,
            AiChatServiceImpl.class
    })
    static class TestApplication {

        @Bean
        public AiBookingSearchTool aiBookingSearchTool() {
            return Mockito.mock(AiBookingSearchTool.class);
        }

        @Bean
        public AiBookingFormTool aiBookingFormTool() {
            return Mockito.mock(AiBookingFormTool.class);
        }

        @Bean
        public AiBookingSubmitTool aiBookingSubmitTool() {
            return Mockito.mock(AiBookingSubmitTool.class);
        }
    }
}
