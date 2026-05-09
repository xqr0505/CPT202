package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiSemanticCacheProperties;
import edu.xjtlu.cpt202.backend.modules.ai.rag.DashScopeEmbeddingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ensures the QA semantic cache vector index exists on startup.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.cache.qa", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSemanticCacheIndexInitializer {

    private final RedisTemplate<String, String> redisTemplate;
    private final AiSemanticCacheProperties cacheProperties;
    private final DashScopeEmbeddingProperties embeddingProperties;

    public AiSemanticCacheIndexInitializer(
            RedisTemplate<String, String> redisTemplate,
            AiSemanticCacheProperties cacheProperties,
            DashScopeEmbeddingProperties embeddingProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
        this.embeddingProperties = embeddingProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndex() {
        try {
            if (indexExists()) {
                return;
            }
            createIndex();
            log.info("Created AI semantic cache index: {}", cacheProperties.getIndexName());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize AI semantic cache index.", exception);
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
                if (cacheProperties.getIndexName().equals(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createIndex() {
        byte[][] args = new byte[][]{
                cacheProperties.getIndexName().getBytes(StandardCharsets.UTF_8),
                "ON".getBytes(StandardCharsets.UTF_8),
                "HASH".getBytes(StandardCharsets.UTF_8),
                "PREFIX".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8),
                cacheProperties.getDocPrefix().getBytes(StandardCharsets.UTF_8),
                "SCHEMA".getBytes(StandardCharsets.UTF_8),
                "query".getBytes(StandardCharsets.UTF_8),
                "TEXT".getBytes(StandardCharsets.UTF_8),
                "answer".getBytes(StandardCharsets.UTF_8),
                "TEXT".getBytes(StandardCharsets.UTF_8),
                "intent".getBytes(StandardCharsets.UTF_8),
                "TAG".getBytes(StandardCharsets.UTF_8),
                "hit_count".getBytes(StandardCharsets.UTF_8),
                "NUMERIC".getBytes(StandardCharsets.UTF_8),
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
                log.info("AI semantic cache index already exists: {}", cacheProperties.getIndexName());
                return;
            }
            throw exception;
        }
    }
}
