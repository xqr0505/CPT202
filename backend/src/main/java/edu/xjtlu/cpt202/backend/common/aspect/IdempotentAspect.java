package edu.xjtlu.cpt202.backend.common.aspect;

import edu.xjtlu.cpt202.backend.common.annotation.Idempotent;
import edu.xjtlu.cpt202.backend.common.constant.IdempotentConstant;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * Aspect handling for @Idempotent requests.
 * @author QiranXiao
 * @date 2026/3/26
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    // in-memory cache for idempotency.
    private final Map<String, Long> idempotentCache = new ConcurrentHashMap<>();

    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    public IdempotentAspect(ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Around("@annotation(idempotent)")
    public Object processIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            userId = IdempotentConstant.ANONYMOUS_USER_ID;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Generate unique key based on userId and endpoint URI
        String cacheKey = buildCacheKey(userId, method, uri);

        if (tryAcquireRedisLock(cacheKey, idempotent.timeout(), idempotent.message())) {
            return joinPoint.proceed();
        }

        return proceedWithInMemoryLock(joinPoint, idempotent, cacheKey);
    }

    private boolean tryAcquireRedisLock(String cacheKey, int timeoutSeconds, String duplicateMessage) {
        RedisTemplate<String, String> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    cacheKey,
                    IdempotentConstant.IDEMPOTENT_VALUE,
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), duplicateMessage);
        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception exception) {
            log.warn("Redis idempotent check failed, fallback to in-memory: key={}, reason={}", cacheKey, exception.getMessage());
            return false;
        }
    }

    private Object proceedWithInMemoryLock(ProceedingJoinPoint joinPoint, Idempotent idempotent, String cacheKey) throws Throwable {
        long currentTime = System.currentTimeMillis();

        Long expireTime = idempotentCache.get(cacheKey);
        if (expireTime != null) {
            if (currentTime < expireTime) {
                throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), idempotent.message());
            }
            idempotentCache.remove(cacheKey);
        }

        idempotentCache.put(cacheKey, currentTime + (idempotent.timeout() * 1000L));
        return joinPoint.proceed();
    }

    private String buildCacheKey(Long userId, String method, String uri) {
        return String.join(
                IdempotentConstant.KEY_SEPARATOR,
                IdempotentConstant.IDEMPOTENT_PREFIX,
                String.valueOf(userId),
                method,
                uri
        );
    }
}
