package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiWorkflowProperties;
import edu.xjtlu.cpt202.backend.modules.ai.model.RescheduleTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleTaskStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of the reschedule workflow state store.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
@Service
public class RedisRescheduleTaskStateStore implements RescheduleTaskStateStore {

    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final AiWorkflowProperties aiWorkflowProperties;

    public RedisRescheduleTaskStateStore(
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> jsonRedisTemplate,
            AiWorkflowProperties aiWorkflowProperties
    ) {
        this.jsonRedisTemplate = jsonRedisTemplate;
        this.aiWorkflowProperties = aiWorkflowProperties;
    }

    @Override
    public Optional<RescheduleTaskState> get(Long userId) {
        Object value = jsonRedisTemplate.opsForValue().get(buildKey(userId));
        if (value instanceof RescheduleTaskState state) {
            refreshTtl(userId);
            return Optional.of(state);
        }
        return Optional.empty();
    }

    @Override
    public void save(Long userId, RescheduleTaskState state) {
        jsonRedisTemplate.opsForValue().set(
                buildKey(userId),
                state,
                aiWorkflowProperties.getRescheduleTtlSeconds(),
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
                aiWorkflowProperties.getRescheduleTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    private String buildKey(Long userId) {
        return aiWorkflowProperties.getRescheduleKeyPrefix() + ":" + userId;
    }
}
