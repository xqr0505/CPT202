package edu.xjtlu.cpt202.backend.modules.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * LangChain4j RAG beans backed by Redis Stack and DashScope embeddings.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Configuration
@ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({RagProperties.class, DashScopeEmbeddingProperties.class})
public class RagConfig {

    @Bean
    public EmbeddingModel embeddingModel(DashScopeEmbeddingProperties properties) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("AI config error: ai.embedding.dashscope.api-key is required (env: DASHSCOPE_API_KEY).");
        }
        return QwenEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            RagProperties ragProperties,
            DashScopeEmbeddingProperties embeddingProperties
    ) {
        RedisEmbeddingStore.Builder builder = RedisEmbeddingStore.builder()
                .host(ragProperties.getRedis().getHost())
                .port(ragProperties.getRedis().getPort())
                .indexName(ragProperties.getRedis().getIndexName())
                .prefix(ragProperties.getRedis().getPrefix())
                .dimension(embeddingProperties.getDimension())
                .metadataKeys(List.of(
                        "documentId",
                        "source",
                        "documentType",
                        "title",
                        "headingPath",
                        "chunkIndex",
                        "ingestedAt"
                ));

        if (StringUtils.hasText(ragProperties.getRedis().getUsername())) {
            builder.user(ragProperties.getRedis().getUsername());
        }
        if (StringUtils.hasText(ragProperties.getRedis().getPassword())) {
            builder.password(ragProperties.getRedis().getPassword());
        }

        return builder.build();
    }

    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            RagProperties ragProperties
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(ragProperties.getMaxResults())
                .minScore(ragProperties.getMinScore())
                .build();
    }

}
