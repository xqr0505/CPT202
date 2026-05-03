package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiSemanticCacheProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

class RedisAiSemanticCacheServiceTest {

    @Test
    void shouldReturnExactMatchWithoutEmbeddingCall() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        @SuppressWarnings("rawtypes")
        HashOperations hashOperations = mock(HashOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

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
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

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
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isEmpty();
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void shouldReturnSemanticHitWhenExactMissAndSimilarityPassesThreshold() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("rawtypes")
        HashOperations hashOperations = mock(HashOperations.class);
        JedisPooled jedis = mock(JedisPooled.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        properties.setSemanticEnabled(true);
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, jedis);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(embeddingModel.embed("refund policy")).thenReturn(Response.from(dev.langchain4j.data.embedding.Embedding.from(new float[]{0.1f, 0.2f})));
        Document document = mock(Document.class);
        when(document.getId()).thenReturn("ai:cache:qa:cache-semantic");
        when(document.get("answer")).thenReturn("semantic answer");
        when(document.get("score")).thenReturn("0.02");
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getDocuments()).thenReturn(List.of(document));
        when(jedis.ftSearch(eq(properties.getIndexName()), any(Query.class))).thenReturn(searchResult);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isPresent();
        assertThat(result.get().exactMatch()).isFalse();
        assertThat(result.get().answer()).isEqualTo("semantic answer");
        assertThat(result.get().score()).isGreaterThanOrEqualTo(0.96D);
    }

    @Test
    void shouldReturnMissWhenSemanticScoreBelowThreshold() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        properties.setSemanticEnabled(true);
        JedisPooled jedis = mock(JedisPooled.class);
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, jedis);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(embeddingModel.embed("refund policy")).thenReturn(Response.from(dev.langchain4j.data.embedding.Embedding.from(new float[]{0.1f, 0.2f})));
        Document document = mock(Document.class);
        when(document.getId()).thenReturn("ai:cache:qa:cache-semantic");
        when(document.get("answer")).thenReturn("semantic answer");
        when(document.get("score")).thenReturn("0.2");
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getDocuments()).thenReturn(List.of(document));
        when(jedis.ftSearch(eq(properties.getIndexName()), any(Query.class))).thenReturn(searchResult);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldWriteExactAndVectorCache() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        @SuppressWarnings("rawtypes")
        HashOperations hashOperations = mock(HashOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        RedisConnection connection = mock(RedisConnection.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(embeddingModel.embed("refund policy")).thenReturn(Response.from(dev.langchain4j.data.embedding.Embedding.from(new float[]{0.1f, 0.2f})));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RedisCallback<Object> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        }).when(redisTemplate).execute(org.mockito.ArgumentMatchers.<RedisCallback<Object>>any());

        service.putAsync("refund policy", "cached answer", AiIntent.KNOWLEDGE);

        await().untilAsserted(() -> {
            verify(hashOperations).putAll(anyString(), any(Map.class));
            verify(connection).execute(eq("HSET"), any(byte[].class), any(byte[].class), any(byte[].class));
            verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(properties.getTtlSeconds())));
            verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofSeconds(properties.getTtlSeconds())));
        });
    }

    @Test
    void shouldSkipSemanticWhenDisabled() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        properties.setSemanticEnabled(false);
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isEmpty();
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void shouldGracefullyDowngradeWhenEmbeddingFails() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        AiSemanticCacheProperties properties = new AiSemanticCacheProperties();
        properties.setSemanticEnabled(true);
        RedisAiSemanticCacheService service = new RedisAiSemanticCacheService(redisTemplate, embeddingModel, properties, new RagProperties());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("embedding down"));

        Optional<AiSemanticCacheService.CacheHit> result = service.get("refund policy", AiIntent.KNOWLEDGE);

        assertThat(result).isEmpty();
    }
}
