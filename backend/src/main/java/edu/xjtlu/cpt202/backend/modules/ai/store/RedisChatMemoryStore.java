package edu.xjtlu.cpt202.backend.modules.ai.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiChatMemoryProperties;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed chat memory store scoped by memory id.
 *
 * @author QiranXiao
 * @since 2026/4/15
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final AiChatMemoryProperties memoryProperties;
    private final AiChatProfiler aiChatProfiler;

    public RedisChatMemoryStore(
            RedisTemplate<String, String> redisTemplate,
            AiChatMemoryProperties memoryProperties,
            AiChatProfiler aiChatProfiler
    ) {
        this.redisTemplate = redisTemplate;
        this.memoryProperties = memoryProperties;
        this.aiChatProfiler = aiChatProfiler;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        long startNs = System.nanoTime();
        String key = buildKey(memoryId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            aiChatProfiler.logStage("memory.getMessages", elapsedMs(startNs), java.util.Map.of(
                    "memoryId", memoryId,
                    "hit", false
            ));
            return Collections.emptyList();
        }

        refreshTtl(key);
        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(payload);
        aiChatProfiler.logStage("memory.getMessages", elapsedMs(startNs), java.util.Map.of(
                "memoryId", memoryId,
                "hit", true,
                "messageCount", messages.size()
        ));
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        long startNs = System.nanoTime();
        String key = buildKey(memoryId);
        String payload = ChatMessageSerializer.messagesToJson(messages);
        Duration ttl = resolveTtl();

        if (ttl != null) {
            redisTemplate.opsForValue().set(key, payload, ttl);
            aiChatProfiler.logStage("memory.updateMessages", elapsedMs(startNs), java.util.Map.of(
                    "memoryId", memoryId,
                    "messageCount", messages.size(),
                    "ttlSeconds", ttl.toSeconds()
            ));
            return;
        }

        redisTemplate.opsForValue().set(key, payload);
        aiChatProfiler.logStage("memory.updateMessages", elapsedMs(startNs), java.util.Map.of(
                "memoryId", memoryId,
                "messageCount", messages.size(),
                "ttlSeconds", null
        ));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        long startNs = System.nanoTime();
        redisTemplate.delete(buildKey(memoryId));
        aiChatProfiler.logStage("memory.deleteMessages", elapsedMs(startNs), java.util.Map.of(
                "memoryId", memoryId
        ));
    }

    private String buildKey(Object memoryId) {
        return memoryProperties.getKeyPrefix() + ":" + String.valueOf(memoryId);
    }

    private void refreshTtl(String key) {
        Duration ttl = resolveTtl();
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    private Duration resolveTtl() {
        return Optional.ofNullable(memoryProperties.getTtlSeconds())
                .filter(ttlSeconds -> ttlSeconds > 0)
                .map(Duration::ofSeconds)
                .orElse(null);
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }
}
