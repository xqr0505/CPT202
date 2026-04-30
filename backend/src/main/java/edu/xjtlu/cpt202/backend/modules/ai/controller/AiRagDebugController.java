package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.ai.constant.AiConstant;
import edu.xjtlu.cpt202.backend.modules.ai.rag.DashScopeEmbeddingProperties;
import edu.xjtlu.cpt202.backend.modules.ai.rag.RagProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.SearchResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lightweight RAG diagnostics endpoint for environment verification.
 */
@RestController
@RequestMapping(AiConstant.API_V1_AI + "/rag")
@ConditionalOnBean(RagProperties.class)
public class AiRagDebugController {

    private final RagProperties ragProperties;
    private final DashScopeEmbeddingProperties embeddingProperties;
    private final Environment environment;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public AiRagDebugController(
            RagProperties ragProperties,
            DashScopeEmbeddingProperties embeddingProperties,
            Environment environment,
            RedisTemplate<String, String> redisTemplate,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        this.ragProperties = ragProperties;
        this.embeddingProperties = embeddingProperties;
        this.environment = environment;
        this.redisTemplate = redisTemplate;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @GetMapping("/diag")
    public Result<Map<String, Object>> diag() {
        RagProperties.Redis redis = ragProperties.getRedis();
        Set<String> keys = redisTemplate.keys(redis.getPrefix() + "*");
        int keyCount = keys == null ? 0 : keys.size();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ragEnabled", ragProperties.isEnabled());
        payload.put("knowledgeLocation", ragProperties.getKnowledgeLocation());
        payload.put("maxResults", ragProperties.getMaxResults());
        payload.put("minScore", ragProperties.getMinScore());
        payload.put("redisHost", redis.getHost());
        payload.put("redisPort", redis.getPort());
        payload.put("redisDatabase", "not-configured");
        payload.put("redisIndexName", redis.getIndexName());
        payload.put("redisPrefix", redis.getPrefix());
        payload.put("segmentKeyCountByPrefix", keyCount);
        payload.put("embeddingModelImpl", embeddingModel.getClass().getName());
        payload.put("embeddingModelNameConfigured", embeddingProperties.getModelName());
        payload.put("embeddingDimensionConfigured", embeddingProperties.getDimension());
        payload.put("springProp_ai_embedding_dashscope_model_name", environment.getProperty("ai.embedding.dashscope.model-name"));
        payload.put("springProp_ai_embedding_dashscope_dimension", environment.getProperty("ai.embedding.dashscope.dimension"));
        payload.put("env_DASHSCOPE_EMBEDDING_MODEL_NAME", System.getenv("DASHSCOPE_EMBEDDING_MODEL_NAME"));
        payload.put("env_DASHSCOPE_EMBEDDING_DIMENSION", System.getenv("DASHSCOPE_EMBEDDING_DIMENSION"));
        return Result.success(payload);
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @GetMapping("/probe")
    public Result<Map<String, Object>> probe(@RequestParam("q") String query) {
        String normalized = query == null ? "" : query.trim();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", normalized);

        if (normalized.isBlank()) {
            payload.put("error", "Query must not be blank.");
            return Result.success(payload);
        }

        Embedding embedding = embeddingModel.embed(normalized).content();
        // Report runtime embedding dimension instead of only config values.
        payload.put("embeddingModelImpl", embeddingModel.getClass().getName());
        payload.put("embeddingModelNameConfigured", embeddingProperties.getModelName());
        payload.put("embeddingDimensionConfigured", embeddingProperties.getDimension());
        payload.put("embeddingDimensionActual", embedding.vector() == null ? null : embedding.vector().length);
        payload.put("springProp_ai_embedding_dashscope_model_name", environment.getProperty("ai.embedding.dashscope.model-name"));
        payload.put("springProp_ai_embedding_dashscope_dimension", environment.getProperty("ai.embedding.dashscope.dimension"));
        payload.put("env_DASHSCOPE_EMBEDDING_MODEL_NAME", System.getenv("DASHSCOPE_EMBEDDING_MODEL_NAME"));
        payload.put("env_DASHSCOPE_EMBEDDING_DIMENSION", System.getenv("DASHSCOPE_EMBEDDING_DIMENSION"));
        payload.put("configuredMinScore", ragProperties.getMinScore());

        EmbeddingSearchResult<TextSegment> configuredResult =
                embeddingStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(embedding)
                        .maxResults(ragProperties.getMaxResults())
                        .minScore(ragProperties.getMinScore())
                        .build());
        EmbeddingSearchResult<TextSegment> noThresholdResult =
                embeddingStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(embedding)
                        .maxResults(ragProperties.getMaxResults())
                        .minScore(0.0)
                        .build());

        payload.put("hitsConfigured", toHits(configuredResult));
        payload.put("hitsNoThreshold", toHits(noThresholdResult));
        payload.put("hitCountConfigured", configuredResult.matches().size());
        payload.put("hitCountNoThreshold", noThresholdResult.matches().size());
        return Result.success(payload);
    }

    @PreAuthorize(AiConstant.AI_CHAT_ACCESS_EXPRESSION)
    @GetMapping("/index")
    public Result<Map<String, Object>> index() {
        Map<String, Object> payload = new LinkedHashMap<>();
        String indexName = ragProperties.getRedis().getIndexName();
        payload.put("indexName", indexName);
        payload.put("redisPrefix", ragProperties.getRedis().getPrefix());
        payload.put("ftInfo", runRedisCommand("FT.INFO", indexName));
        payload.put("ftSearchAll", runRedisCommand("FT.SEARCH", indexName, "*", "LIMIT", "0", "3"));
        payload.put("jedisIndex", runJedisIndexProbe(indexName));
        return Result.success(payload);
    }

    private List<Map<String, Object>> toHits(EmbeddingSearchResult<TextSegment> result) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            TextSegment segment = match.embedded();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("score", match.score());
            item.put("source", segment.metadata("source"));
            item.put("headingPath", segment.metadata("headingPath"));
            item.put("chunkIndex", segment.metadata("chunkIndex"));
            String text = segment.text();
            if (text != null && text.length() > 160) {
                text = text.substring(0, 160) + "...";
            }
            item.put("preview", text);
            items.add(item);
        }
        return items;
    }

    private Object runRedisCommand(String... args) {
        try {
            return redisTemplate.execute((RedisConnection connection) -> {
                byte[][] commandArgs = new byte[args.length - 1][];
                for (int index = 1; index < args.length; index++) {
                    commandArgs[index - 1] = args[index].getBytes(StandardCharsets.UTF_8);
                }
                Object reply = connection.execute(args[0], commandArgs);
                return normalizeRedisReply(reply);
            });
        } catch (Exception exception) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("command", String.join(" ", args));
            error.put("errorType", exception.getClass().getName());
            error.put("message", exception.getMessage());
            Throwable root = exception;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            error.put("rootErrorType", root.getClass().getName());
            error.put("rootMessage", root.getMessage());
            return error;
        }
    }

    private Object normalizeRedisReply(Object reply) {
        if (reply == null) {
            return null;
        }
        if (reply instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (reply instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalizeRedisReply(item));
            }
            return normalized;
        }
        if (reply instanceof Set<?> set) {
            List<Object> normalized = new ArrayList<>(set.size());
            for (Object item : set) {
                normalized.add(normalizeRedisReply(item));
            }
            return normalized;
        }
        if (reply instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(Objects.toString(normalizeRedisReply(entry.getKey()), "null"),
                        normalizeRedisReply(entry.getValue()));
            }
            return normalized;
        }
        if (reply.getClass().isArray()) {
            return Collections.singletonList(reply.toString());
        }
        return reply;
    }

    private Map<String, Object> runJedisIndexProbe(String indexName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        RagProperties.Redis redis = ragProperties.getRedis();
        try (JedisPooled jedis = createJedisClient(redis)) {
            payload.put("ftList", jedis.ftList());
            payload.put("ftInfo", jedis.ftInfo(indexName));
            SearchResult searchResult = jedis.ftSearch(indexName, "*", FTSearchParams.searchParams().limit(0, 3));
            payload.put("totalResults", searchResult.getTotalResults());
            payload.put("documents", toDocuments(searchResult.getDocuments()));
        } catch (Exception exception) {
            Throwable root = exception;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            payload.put("errorType", exception.getClass().getName());
            payload.put("message", exception.getMessage());
            payload.put("rootErrorType", root.getClass().getName());
            payload.put("rootMessage", root.getMessage());
        }
        return payload;
    }

    private JedisPooled createJedisClient(RagProperties.Redis redis) {
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

    private List<Map<String, Object>> toDocuments(List<Document> documents) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document document : documents) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", document.getId());
            item.put("score", document.getScore());
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : document.getProperties()) {
                Object value = entry.getValue();
                properties.put(entry.getKey(), value instanceof byte[] bytes ? "<bytes:" + bytes.length + ">" : value);
            }
            item.put("properties", properties);
            result.add(item);
        }
        return result;
    }
}
