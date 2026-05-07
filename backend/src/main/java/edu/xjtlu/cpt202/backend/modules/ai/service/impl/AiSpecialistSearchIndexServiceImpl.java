package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiSpecialistSearchProperties;
import edu.xjtlu.cpt202.backend.modules.ai.mapper.AiSpecialistSearchSyncMapper;
import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchIndexDocument;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSpecialistSearchIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Syncs specialist documents into Redis hybrid-search index.
 *
 * @author Codex
 * @since 2026/5/7
 */
@Service
public class AiSpecialistSearchIndexServiceImpl implements AiSpecialistSearchIndexService {

    private static final String REDIS_HSET = "HSET";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_LEVEL = "level";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_VECTOR = "vector";

    private final RedisTemplate<String, String> redisTemplate;
    private final EmbeddingModel embeddingModel;
    private final AiSpecialistSearchProperties properties;
    private final AiSpecialistSearchSyncMapper syncMapper;

    @Autowired
    public AiSpecialistSearchIndexServiceImpl(
            RedisTemplate<String, String> redisTemplate,
            EmbeddingModel embeddingModel,
            AiSpecialistSearchProperties properties,
            AiSpecialistSearchSyncMapper syncMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.embeddingModel = embeddingModel;
        this.properties = properties;
        this.syncMapper = syncMapper;
    }

    @Override
    public void rebuildAll() {
        List<AiSpecialistSearchIndexDocument> documents = syncMapper.selectAll();
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (AiSpecialistSearchIndexDocument document : documents) {
            upsertDocumentIfEligible(document);
        }
    }

    @Override
    public void upsertSpecialist(Long specialistId) {
        if (specialistId == null) {
            return;
        }
        AiSpecialistSearchIndexDocument document = syncMapper.selectBySpecialistId(specialistId);
        if (document == null) {
            deleteSpecialist(specialistId);
            return;
        }
        upsertDocumentIfEligible(document);
    }

    @Override
    public void deleteSpecialist(Long specialistId) {
        if (specialistId == null) {
            return;
        }
        redisTemplate.delete(documentKey(specialistId));
    }

    private void upsertDocumentIfEligible(AiSpecialistSearchIndexDocument document) {
        if (document == null || document.getSpecialistId() == null) {
            return;
        }
        if (!properties.isIncludeInactive() && !"ACTIVE".equalsIgnoreCase(trimToEmpty(document.getStatus()))) {
            deleteSpecialist(document.getSpecialistId());
            return;
        }

        String content = buildContent(document);
        document.setContent(content);
        Embedding embedding = embed(content);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put(FIELD_ID, document.getSpecialistId().toString());
        payload.put(FIELD_NAME, trimToEmpty(document.getSpecialistName()));
        payload.put(FIELD_CATEGORY, trimToEmpty(document.getCategoryName()));
        payload.put(FIELD_LEVEL, trimToEmpty(document.getLevel()).toUpperCase());
        payload.put(FIELD_CONTENT, content);

        String key = documentKey(document.getSpecialistId());
        redisTemplate.opsForHash().putAll(key, payload);
        writeVectorBinary(key, toLittleEndianBytes(embedding.vector()));
    }

    private Embedding embed(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        if (response == null || response.content() == null || response.content().vector() == null) {
            throw new IllegalStateException("Specialist embedding result is empty.");
        }
        return response.content();
    }

    private String buildContent(AiSpecialistSearchIndexDocument document) {
        return "Specialist: " + trimToEmpty(document.getSpecialistName())
                + ". Category: " + trimToEmpty(document.getCategoryName())
                + ". Level: " + trimToEmpty(document.getLevel()).toUpperCase()
                + ". Bio: " + trimToEmpty(document.getBio());
    }

    private void writeVectorBinary(String docKey, byte[] vectorBytes) {
        byte[][] args = new byte[][]{
                docKey.getBytes(StandardCharsets.UTF_8),
                FIELD_VECTOR.getBytes(StandardCharsets.UTF_8),
                vectorBytes
        };
        redisTemplate.execute((RedisConnection connection) -> connection.execute(REDIS_HSET, args));
    }

    private byte[] toLittleEndianBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private String documentKey(Long specialistId) {
        return properties.getDocPrefix() + specialistId;
    }

    private String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
