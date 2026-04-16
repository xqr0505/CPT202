package edu.xjtlu.cpt202.backend.modules.ai.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
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

    public RedisChatMemoryStore(
            RedisTemplate<String, String> redisTemplate,
            AiChatMemoryProperties memoryProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.memoryProperties = memoryProperties;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }

        refreshTtl(key);
        return ChatMessageDeserializer.messagesFromJson(payload);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        String payload = ChatMessageSerializer.messagesToJson(messages);
        Duration ttl = resolveTtl();

        if (ttl != null) {
            redisTemplate.opsForValue().set(key, payload, ttl);
            return;
        }

        redisTemplate.opsForValue().set(key, payload);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(buildKey(memoryId));
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
}
