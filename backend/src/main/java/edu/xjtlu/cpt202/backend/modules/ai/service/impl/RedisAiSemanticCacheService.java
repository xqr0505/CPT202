package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiSemanticCacheProperties;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

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
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_SCORE_ALIAS = "score";
    private static final String REDIS_HSET = "HSET";
    private static final String REDIS_FT_SEARCH = "FT.SEARCH";

    private final RedisTemplate<String, String> redisTemplate;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
    private final AiSemanticCacheProperties properties;
    private final JedisPooled qaSearchJedis;

    @Autowired
    public RedisAiSemanticCacheService(
            RedisTemplate<String, String> redisTemplate,
            dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
            AiSemanticCacheProperties properties,
            RagProperties ragProperties
    ) {
        this(redisTemplate, embeddingModel, properties, createJedisClientStatic(ragProperties == null ? null : ragProperties.getRedis()));
    }

    RedisAiSemanticCacheService(
            RedisTemplate<String, String> redisTemplate,
            dev.langchain4j.model.embedding.EmbeddingModel embeddingModel,
            AiSemanticCacheProperties properties,
            JedisPooled qaSearchJedis
    ) {
        this.redisTemplate = redisTemplate;
        this.embeddingModel = embeddingModel;
        this.properties = properties;
        this.qaSearchJedis = qaSearchJedis;
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
                return findBySemantic(normalizedQuery, intent);
            }

            Object answer = redisTemplate.opsForHash().get(documentKey(cacheId), FIELD_ANSWER);
            if (!(answer instanceof String answerText) || answerText.isBlank()) {
                return findBySemantic(normalizedQuery, intent);
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
            byte[] vectorBytes = toLittleEndianBytes(embed(query).vector());

            Map<String, String> payload = new LinkedHashMap<>();
            payload.put(FIELD_QUERY, query);
            payload.put(FIELD_ANSWER, answer);
            payload.put(FIELD_INTENT, intent.name());
            payload.put(FIELD_HIT_COUNT, "1");

            redisTemplate.opsForHash().putAll(documentKey, payload);
            writeVectorBinary(documentKey, vectorBytes);
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

    private Optional<CacheHit> findBySemantic(String query, AiIntent intent) {
        if (!properties.isSemanticEnabled() || intent != AiIntent.KNOWLEDGE) {
            return Optional.empty();
        }
        try {
            byte[] vector = toLittleEndianBytes(embed(query).vector());
            SemanticCandidate candidate = searchTopCandidate(vector, intent);
            if (candidate == null || candidate.answer().isBlank()) {
                return Optional.empty();
            }
            if (candidate.score() < properties.getSimilarityThreshold()) {
                return Optional.empty();
            }
            incrementHitCount(documentKey(candidate.cacheId()));
            return Optional.of(new CacheHit(candidate.cacheId(), candidate.answer(), false, candidate.score()));
        } catch (Exception exception) {
            log.debug(
                    "Semantic QA cache get failed, fallback to miss. index={}, intent={}, threshold={}, rootCause={}",
                    properties.getIndexName(),
                    intent,
                    properties.getSimilarityThreshold(),
                    rootCauseMessage(exception)
            );
            return Optional.empty();
        }
    }

    private Embedding embed(String query) {
        Response<Embedding> response = embeddingModel.embed(query);
        if (response == null || response.content() == null || response.content().vector() == null) {
            throw new IllegalStateException("Embedding result is empty.");
        }
        return response.content();
    }

    private void writeVectorBinary(String docKey, byte[] vectorBytes) {
        byte[][] args = new byte[][]{
                docKey.getBytes(StandardCharsets.UTF_8),
                FIELD_VECTOR.getBytes(StandardCharsets.UTF_8),
                vectorBytes
        };
        redisTemplate.execute((RedisConnection connection) -> connection.execute(REDIS_HSET, args));
    }

    private SemanticCandidate searchTopCandidate(byte[] vectorBytes, AiIntent intent) {
        int topK = Math.max(properties.getMaxResults(), 1);
        String query = "@intent:{" + intent.name() + "}=>[KNN $K @" + FIELD_VECTOR + " $BLOB AS " + FIELD_SCORE_ALIAS + "]";
        log.debug("Semantic QA FT.SEARCH request. index={}, query={}, topK={}, vectorBytes={}",
                properties.getIndexName(), query, topK, vectorBytes.length);
        Query qaQuery = new Query(query)
                .addParam("K", topK)
                .addParam("BLOB", vectorBytes)
                .setSortBy(FIELD_SCORE_ALIAS, true)
                .limit(0, topK)
                .returnFields(FIELD_ANSWER, FIELD_SCORE_ALIAS)
                .dialect(2);
        SearchResult result = qaSearchJedis.ftSearch(properties.getIndexName(), qaQuery);
        return parseTopCandidate(result);
    }

    private SemanticCandidate parseTopCandidate(SearchResult result) {
        if (result == null || result.getDocuments() == null || result.getDocuments().isEmpty()) {
            return null;
        }
        Document document = result.getDocuments().get(0);
        String docId = document.getId();
        if (docId == null || docId.isBlank()) {
            return null;
        }
        String answer = valueAsString(document.get(FIELD_ANSWER));
        String scoreRaw = valueAsString(document.get(FIELD_SCORE_ALIAS));
        if (scoreRaw == null || answer.isBlank()) {
            return null;
        }
        double distance = Double.parseDouble(scoreRaw);
        double similarity = 1.0D - distance;
        return new SemanticCandidate(extractCacheId(docId), answer, similarity);
    }

    private String decode(Object raw) {
        if (raw instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (raw instanceof String string) {
            return string;
        }
        return null;
    }

    private String extractCacheId(String docId) {
        String prefix = properties.getDocPrefix();
        if (docId.startsWith(prefix)) {
            return docId.substring(prefix.length());
        }
        return docId;
    }

    private byte[] toLittleEndianBytes(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalStateException("Embedding vector is empty.");
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private record SemanticCandidate(
            String cacheId,
            String answer,
            double score
    ) {
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : (": " + message));
    }

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    protected JedisPooled createJedisClient(RagProperties.Redis redis) {
        return createJedisClientStatic(redis);
    }

    private static JedisPooled createJedisClientStatic(RagProperties.Redis redis) {
        if (redis == null) {
            return new JedisPooled("127.0.0.1", 6379);
        }
        String username = redis.getUsername();
        String password = redis.getPassword();
        if (username != null && !username.isBlank()) {
            return new JedisPooled(redis.getHost(), redis.getPort(), username, password);
        }
        if (password != null && !password.isBlank()) {
            return new JedisPooled(redis.getHost(), redis.getPort(), null, password);
        }
        return new JedisPooled(redis.getHost(), redis.getPort());
    }
}
