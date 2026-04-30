package edu.xjtlu.cpt202.backend.modules.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rebuilds the Redis vector index from Markdown knowledge files on startup.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagIngestionService {

    private static final String KNOWLEDGE_ROOT = "knowledge/";

    private final RagProperties ragProperties;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final MarkdownHeadingSegmenter segmenter;
    private final RedisTemplate<String, String> redisTemplate;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public RagIngestionService(
            RagProperties ragProperties,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            MarkdownHeadingSegmenter segmenter,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.ragProperties = ragProperties;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.segmenter = segmenter;
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildKnowledgeBase() {
        try {
            Resource[] resources = resourceResolver.getResources(ragProperties.getKnowledgeLocation());
            
            StringBuilder contentBuilder = new StringBuilder();
            List<Object[]> pendingResources = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    log.warn("Skipping unreadable RAG resource: {}", resource);
                    continue;
                }
                String markdown = loadMarkdown(resource);
                contentBuilder.append(markdown);
                pendingResources.add(new Object[]{resource, markdown});
            }
            
            String currentHash = DigestUtils.md5DigestAsHex(contentBuilder.toString().getBytes(StandardCharsets.UTF_8));
            String hashKey = ragProperties.getRedis().getIndexName() + ":content_hash";
            String storedHash = redisTemplate.opsForValue().get(hashKey);
            
            if (currentHash.equals(storedHash)) {
                log.info("RAG knowledge index is up-to-date, skipping rebuild. Hash: {}", currentHash);
                return;
            }

            log.info("Rebuilding RAG knowledge index from {} resource(s): {}", resources.length, ragProperties.getKnowledgeLocation());
            clearRedisIndex();

            List<TextSegment> segments = new ArrayList<>();
            for (Object[] pair : pendingResources) {
                Resource resource = (Resource) pair[0];
                String markdown = (String) pair[1];
                String source = buildSource(resource);
                MarkdownHeadingSegmenter.DocumentMetadata metadata = segmenter.buildMetadata(source, markdown);
                List<TextSegment> documentSegments = segmenter.split(markdown, metadata);
                segments.addAll(documentSegments);
                log.info("Prepared {} RAG segment(s) from {}", documentSegments.size(), source);
            }

            if (segments.isEmpty()) {
                log.warn("No RAG knowledge segments were created. Check ai.rag.knowledge-location.");
                return;
            }

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            
            redisTemplate.opsForValue().set(hashKey, currentHash);
            log.info("RAG knowledge index rebuilt successfully with {} segment(s). Hash: {}", segments.size(), currentHash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to rebuild RAG knowledge index.", ex);
        }
    }

    private String loadMarkdown(Resource resource) throws IOException {
        if (resource.isFile()) {
            Document document = FileSystemDocumentLoader.loadDocument(resource.getFile().toPath(), new TextDocumentParser());
            return document.text();
        }
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String buildSource(Resource resource) throws IOException {
        String url = resource.getURL().toString().replace('\\', '/');
        int knowledgeIndex = url.indexOf(KNOWLEDGE_ROOT);
        if (knowledgeIndex >= 0) {
            return url.substring(knowledgeIndex);
        }
        String filename = resource.getFilename();
        return KNOWLEDGE_ROOT + (filename == null ? "unknown.md" : filename);
    }

    private void clearRedisIndex() {
        Set<String> segmentKeys = redisTemplate.keys(ragProperties.getRedis().getPrefix() + "*");
        if (segmentKeys != null && !segmentKeys.isEmpty()) {
            redisTemplate.delete(segmentKeys);
        }
        redisTemplate.delete(ragProperties.getRedis().getIndexName());
    }
}
