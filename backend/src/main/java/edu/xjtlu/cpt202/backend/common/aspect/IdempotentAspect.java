package edu.xjtlu.cpt202.backend.common.aspect;

import edu.xjtlu.cpt202.backend.common.annotation.Idempotent;
import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Aspect handling for @Idempotent requests.
 * @author QiranXiao
 * @date 2026/3/26
 */
@Aspect
@Component
public class IdempotentAspect {

    // in-memory cache for idempotency.
    private final Map<String, Long> idempotentCache = new ConcurrentHashMap<>();

    @Around("@annotation(idempotent)")
    public Object processIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            userId = -1L;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String uri = request.getRequestURI();

        // Generate unique key based on userId and endpoint URI
        String cacheKey = "idempotent:" + userId + ":" + uri;
        long currentTime = System.currentTimeMillis();

        // Expire check and lock acquisition
        Long expireTime = idempotentCache.get(cacheKey);
        if (expireTime != null && currentTime < expireTime) {
            // Re-submit within timeout
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), idempotent.message());
        }

        // Set new timeout (in memory)
        idempotentCache.put(cacheKey, currentTime + (idempotent.timeout() * 1000L));

        try {
            return joinPoint.proceed();
        } finally {
            // Wait for time to expire naturally
        }
    }
}
