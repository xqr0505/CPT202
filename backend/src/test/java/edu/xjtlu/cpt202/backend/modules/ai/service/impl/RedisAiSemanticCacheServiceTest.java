package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.model.embedding.EmbeddingModel;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiSemanticCacheProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAiSemanticCacheServiceTest {

    @Test
    void shouldReturnExactMatchWithoutEmbeddingCall() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        @SuppressWarnings("rawtypes")
        HashOperations hashOperations = mock(HashOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(anyString())).thenReturn("cache-1");
        when(hashOperations.get("ai:cache:qa:cache-1", "answer")).thenReturn("cached answer");

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isPresent();
        assertThat(result.get().answer()).isEqualTo("cached answer");
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void shouldSkipNonKnowledgeIntent() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.BOOKING);

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldReturnMissWhenExactKeyDoesNotExist() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isEmpty();
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void shouldWriteExactCacheWithoutEmbeddingCall() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        @SuppressWarnings("rawtypes")
        HashOperations hashOperations = mock(HashOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        service.putAsync("refund policy", "cached answer", AiIntent.KNOWLEDGE);

        verify(embeddingModel, never()).embed(anyString());
        await().untilAsserted(() -> {
            verify(hashOperations).putAll(anyString(), any(Map.class));
            verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(properties.getTtlSeconds())));
            verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofSeconds(properties.getTtlSeconds())));
        });
    }
}
