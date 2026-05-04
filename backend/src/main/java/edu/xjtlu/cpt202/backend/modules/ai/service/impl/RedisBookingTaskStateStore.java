package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiWorkflowProperties;
import edu.xjtlu.cpt202.backend.modules.ai.model.BookingTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.BookingTaskStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of the booking workflow state store.
 *
 * @author QiranXiao
 * @since 2026/5/5
 */
@Service
public class RedisBookingTaskStateStore implements BookingTaskStateStore {

    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final AiWorkflowProperties aiWorkflowProperties;

    public RedisBookingTaskStateStore(
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> jsonRedisTemplate,
            AiWorkflowProperties aiWorkflowProperties
    ) {
        this.jsonRedisTemplate = jsonRedisTemplate;
        this.aiWorkflowProperties = aiWorkflowProperties;
    }

    @Override
    public Optional<BookingTaskState> get(Long userId) {
        Object value = jsonRedisTemplate.opsForValue().get(buildKey(userId));
        if (value instanceof BookingTaskState state) {
            refreshTtl(userId);
            return Optional.of(state);
        }
        return Optional.empty();
    }

    @Override
    public void save(Long userId, BookingTaskState state) {
        jsonRedisTemplate.opsForValue().set(
                buildKey(userId),
                state,
                aiWorkflowProperties.getBookingTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void clear(Long userId) {
        jsonRedisTemplate.delete(buildKey(userId));
    }

    private void refreshTtl(Long userId) {
        jsonRedisTemplate.expire(
                buildKey(userId),
                aiWorkflowProperties.getBookingTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    private String buildKey(Long userId) {
        return aiWorkflowProperties.getBookingKeyPrefix() + ":" + userId;
    }
}
