package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiSemanticCacheProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis-backed exact QA cache for knowledge answers.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
@Slf4j
@Service
public class RedisAiSemanticCacheService implements AiSemanticCacheService {

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_ANSWER = "answer";
    private static final String FIELD_INTENT = "intent";
    private static final String FIELD_HIT_COUNT = "hit_count";

    private final RedisTemplate<String, String> redisTemplate;
    private final AiSemanticCacheProperties properties;

    public RedisAiSemanticCacheService(
            RedisTemplate<String, String> redisTemplate,
            dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
            AiSemanticCacheProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<CacheHit> get(String query, AiIntent intent) {
        if (!properties.isEnabled() || query == null || query.isBlank() || intent != AiIntent.KNOWLEDGE) {
            return Optional.empty();
        }

        try {
            String normalizedQuery = query.trim();
            String exactKey = exactKey(normalizedQuery);
            String cacheId = redisTemplate.opsForValue().get(exactKey);
            if (cacheId == null || cacheId.isBlank()) {
                return Optional.empty();
            }

            Object answer = redisTemplate.opsForHash().get(documentKey(cacheId), FIELD_ANSWER);
            if (!(answer instanceof String answerText) || answerText.isBlank()) {
                return Optional.empty();
            }

            incrementHitCount(documentKey(cacheId));
            return Optional.of(new CacheHit(cacheId, answerText, true, 1.0D));
        } catch (Exception exception) {
            log.debug("Exact QA cache get failed, fallback to miss: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void putAsync(String query, String answer, AiIntent intent) {
        if (!properties.isEnabled() || intent != AiIntent.KNOWLEDGE) {
            return;
        }
        if (query == null || query.isBlank() || answer == null || answer.isBlank()) {
            return;
        }

        String normalizedQuery = query.trim();
        String normalizedAnswer = answer.trim();
        CompletableFuture.runAsync(() -> putInternal(normalizedQuery, normalizedAnswer, intent));
    }

    @Override
    public void clearAll() {
        if (!properties.isEnabled()) {
            return;
        }
        var docKeys = redisTemplate.keys(properties.getDocPrefix() + "*");
        if (docKeys != null && !docKeys.isEmpty()) {
            redisTemplate.delete(docKeys);
        }
        var exactKeys = redisTemplate.keys(properties.getExactPrefix() + "*");
        if (exactKeys != null && !exactKeys.isEmpty()) {
            redisTemplate.delete(exactKeys);
        }
    }

    private void putInternal(String query, String answer, AiIntent intent) {
        try {
            String exactKey = exactKey(query);
            String existingCacheId = redisTemplate.opsForValue().get(exactKey);
            String cacheId = (existingCacheId == null || existingCacheId.isBlank()) ? UUID.randomUUID().toString() : existingCacheId;
            String documentKey = documentKey(cacheId);

            Map<String, String> payload = new LinkedHashMap<>();
            payload.put(FIELD_QUERY, query);
            payload.put(FIELD_ANSWER, answer);
            payload.put(FIELD_INTENT, intent.name());
            payload.put(FIELD_HIT_COUNT, "1");

            redisTemplate.opsForHash().putAll(documentKey, payload);
            redisTemplate.expire(documentKey, Duration.ofSeconds(properties.getTtlSeconds()));
            redisTemplate.opsForValue().set(exactKey, cacheId, Duration.ofSeconds(properties.getTtlSeconds()));
        } catch (Exception exception) {
            log.debug("Exact QA cache put failed: {}", exception.getMessage());
        }
    }

    private void incrementHitCount(String documentKey) {
        try {
            redisTemplate.opsForHash().increment(documentKey, FIELD_HIT_COUNT, 1L);
        } catch (Exception exception) {
            log.debug("Exact QA cache hit count increment failed: {}", exception.getMessage());
        }
    }

    private String exactKey(String query) {
        return properties.getExactPrefix() + DigestUtils.md5DigestAsHex(query.getBytes(StandardCharsets.UTF_8));
    }

    private String documentKey(String cacheId) {
        return properties.getDocPrefix() + cacheId;
    }
}
