package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.config.AiWorkflowProperties;
import edu.xjtlu.cpt202.backend.modules.ai.model.BookingTaskState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBookingTaskStateStoreTest {

    @Test
    void get_ReturnsEmptyAndClearsKeyWhenDeserializeFails() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq("expertlink:ai:workflow:booking:33")))
                .thenThrow(new IllegalArgumentException("bad payload"));

        AiWorkflowProperties properties = new AiWorkflowProperties();
        RedisBookingTaskStateStore store = new RedisBookingTaskStateStore(redisTemplate, properties);

        Optional<BookingTaskState> result = store.get(33L);

        assertTrue(result.isEmpty());
        verify(redisTemplate).delete("expertlink:ai:workflow:booking:33");
        verify(redisTemplate, never()).expire(any(), anyLong(), any());
    }

    @Test
    void get_ReturnsStateAndRefreshesTtlWhenPayloadIsValid() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        BookingTaskState state = BookingTaskState.builder().taskStateText("active").build();
        when(valueOperations.get(eq("expertlink:ai:workflow:booking:44"))).thenReturn(state);

        AiWorkflowProperties properties = new AiWorkflowProperties();
        properties.setBookingTtlSeconds(1200L);
        RedisBookingTaskStateStore store = new RedisBookingTaskStateStore(redisTemplate, properties);

        Optional<BookingTaskState> result = store.get(44L);

        assertTrue(result.isPresent());
        assertEquals("active", result.get().getTaskStateText());
        verify(redisTemplate).expire("expertlink:ai:workflow:booking:44", 1200L, java.util.concurrent.TimeUnit.SECONDS);
    }
}
