package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiSpecialistSearchProperties;
import edu.xjtlu.cpt202.backend.modules.ai.rag.DashScopeEmbeddingProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSpecialistSearchIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ensures specialist-search Redis index exists and is populated.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.search.specialist", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSpecialistSearchIndexInitializer {

    private final RedisTemplate<String, String> redisTemplate;
    private final AiSpecialistSearchProperties properties;
    private final DashScopeEmbeddingProperties embeddingProperties;
    private final AiSpecialistSearchIndexService indexService;

    public AiSpecialistSearchIndexInitializer(
            RedisTemplate<String, String> redisTemplate,
            AiSpecialistSearchProperties properties,
            DashScopeEmbeddingProperties embeddingProperties,
            AiSpecialistSearchIndexService indexService
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.embeddingProperties = embeddingProperties;
        this.indexService = indexService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexAndSync() {
        try {
            if (!indexExists()) {
                createIndex();
                log.info("Created AI specialist search index: {}", properties.getIndexName());
            }
            indexService.rebuildAll();
            log.info("AI specialist search index sync completed: {}", properties.getIndexName());
        } catch (Exception exception) {
            log.error("AI specialist search index sync failed, continue startup in degraded mode. index={}",
                    properties.getIndexName(), exception);
        }
    }

    private boolean indexExists() {
        Object reply = redisTemplate.execute((RedisConnection connection) ->
                connection.execute("FT._LIST", new byte[0][])
        );
        if (!(reply instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (item instanceof byte[] bytes) {
                String indexName = new String(bytes, StandardCharsets.UTF_8);
                if (properties.getIndexName().equals(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createIndex() {
        byte[][] args = new byte[][]{
                properties.getIndexName().getBytes(StandardCharsets.UTF_8),
                "ON".getBytes(StandardCharsets.UTF_8),
                "HASH".getBytes(StandardCharsets.UTF_8),
                "PREFIX".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8),
                properties.getDocPrefix().getBytes(StandardCharsets.UTF_8),
                "SCHEMA".getBytes(StandardCharsets.UTF_8),
                "id".getBytes(StandardCharsets.UTF_8),
                "TAG".getBytes(StandardCharsets.UTF_8),
                "name".getBytes(StandardCharsets.UTF_8),
                "TEXT".getBytes(StandardCharsets.UTF_8),
                "category".getBytes(StandardCharsets.UTF_8),
                "TAG".getBytes(StandardCharsets.UTF_8),
                "level".getBytes(StandardCharsets.UTF_8),
                "TAG".getBytes(StandardCharsets.UTF_8),
                "content".getBytes(StandardCharsets.UTF_8),
                "TEXT".getBytes(StandardCharsets.UTF_8),
                "vector".getBytes(StandardCharsets.UTF_8),
                "VECTOR".getBytes(StandardCharsets.UTF_8),
                "FLAT".getBytes(StandardCharsets.UTF_8),
                "6".getBytes(StandardCharsets.UTF_8),
                "TYPE".getBytes(StandardCharsets.UTF_8),
                "FLOAT32".getBytes(StandardCharsets.UTF_8),
                "DIM".getBytes(StandardCharsets.UTF_8),
                Integer.toString(embeddingProperties.getDimension()).getBytes(StandardCharsets.UTF_8),
                "DISTANCE_METRIC".getBytes(StandardCharsets.UTF_8),
                "COSINE".getBytes(StandardCharsets.UTF_8)
        };
        try {
            redisTemplate.execute((RedisConnection connection) -> connection.execute("FT.CREATE", args));
        } catch (RedisSystemException exception) {
            String message = exception.getMostSpecificCause() == null
                    ? exception.getMessage()
                    : exception.getMostSpecificCause().getMessage();
            if (message != null && message.toLowerCase().contains("index already exists")) {
                log.info("AI specialist search index already exists: {}", properties.getIndexName());
                return;
            }
            throw exception;
        }
    }
}
