package edu.xjtlu.cpt202.backend.common.aspect;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logs controller and service method calls with conservative argument summaries.
 *
 * @author OpenAI
 * @since 2026/5/1
 */
@Slf4j
@Aspect
@Component
public class MethodLogAspect {

    private static final String CONTROLLER_POINTCUT =
            "execution(* edu.xjtlu.cpt202.backend..controller..*(..)) || within(@org.springframework.web.bind.annotation.RestController *)";
    private static final String SERVICE_POINTCUT =
            "execution(* edu.xjtlu.cpt202.backend..service..*(..)) || within(@org.springframework.stereotype.Service *)";
    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "error";

    private final CommonProperties commonProperties;

    public MethodLogAspect(CommonProperties commonProperties) {
        this.commonProperties = commonProperties;
    }

    @Around(CONTROLLER_POINTCUT + " || " + SERVICE_POINTCUT)
    public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!commonProperties.getLogging().isEnabled()) {
            return joinPoint.proceed();
        }

        long startNs = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String layer = resolveLayer(method.getDeclaringClass());
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String argsSummary = summarizeArguments(signature.getParameterNames(), joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long costMs = elapsedMs(startNs);
            logSuccess(layer, className, methodName, costMs, argsSummary, summarizeResult(result));
            return result;
        } catch (Throwable throwable) {
            long costMs = elapsedMs(startNs);
            logError(layer, className, methodName, costMs, argsSummary, throwable);
            throw throwable;
        }
    }

    private void logSuccess(
            String layer,
            String className,
            String methodName,
            long costMs,
            String argsSummary,
            String resultSummary
    ) {
        String message = "method={}#{} layer={} status={} costMs={} args={} result={} {}";
        Object[] values = new Object[]{
                className,
                methodName,
                layer,
                SUCCESS_STATUS,
                costMs,
                argsSummary,
                resultSummary,
                requestSummary()
        };
        if (isSlowCall(layer, costMs)) {
            log.warn(message, values);
            return;
        }
        log.info(message, values);
    }

    private void logError(
            String layer,
            String className,
            String methodName,
            long costMs,
            String argsSummary,
            Throwable throwable
    ) {
        log.error(
                "method={}#{} layer={} status={} costMs={} args={} exception={} message={} {}",
                className,
                methodName,
                layer,
                ERROR_STATUS,
                costMs,
                argsSummary,
                throwable.getClass().getSimpleName(),
                truncate(throwable.getMessage()),
                requestSummary(),
                throwable
        );
    }

    private boolean isSlowCall(String layer, long costMs) {
        CommonProperties.SlowThreshold slowThreshold = commonProperties.getLogging().getSlowThreshold();
        if ("controller".equals(layer)) {
            return costMs >= slowThreshold.getControllerMs();
        }
        return costMs >= slowThreshold.getServiceMs();
    }

    private String summarizeArguments(String[] parameterNames, Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            String parameterName = parameterNames != null && index < parameterNames.length
                    ? parameterNames[index]
                    : "arg" + index;
            summary.put(parameterName, summarizeValue(args[index]));
        }
        return truncate(summary.toString());
    }

    private String summarizeResult(Object result) {
        return summarizeValue(result);
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + truncate(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean || value.getClass().isEnum()) {
            return String.valueOf(value);
        }
        if (value instanceof MultipartFile multipartFile) {
            return "MultipartFile[name=%s,size=%d]".formatted(
                    truncate(multipartFile.getOriginalFilename()),
                    multipartFile.getSize()
            );
        }
        if (value instanceof ServletRequest) {
            return "<ServletRequest>";
        }
        if (value instanceof ServletResponse) {
            return "<ServletResponse>";
        }
        if (value instanceof SseEmitter) {
            return "<SseEmitter>";
        }
        if (value instanceof InputStream) {
            return "<InputStream>";
        }
        if (value instanceof OutputStream) {
            return "<OutputStream>";
        }
        if (value instanceof byte[] bytes) {
            return "<byte[%d]>".formatted(bytes.length);
        }
        if (value.getClass().isArray()) {
            return summarizeArray(value);
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }
        return truncate(value.toString());
    }

    private String summarizeArray(Object array) {
        int length = Array.getLength(array);
        Class<?> componentType = array.getClass().componentType();
        if (componentType != null && componentType.isPrimitive()) {
            return "<%s[%d]>".formatted(componentType.getSimpleName(), length);
        }
        Object[] objectArray = Arrays.copyOf((Object[]) array, Math.min(length, 3));
        return componentType == null
                ? "Array(length=" + length + ")"
                : componentType.getSimpleName() + "[](length=" + length + ",sample=" + Arrays.toString(objectArray) + ")";
    }

    private String requestSummary() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "requestContext=none";
        }

        Long userId = UserContextHolder.getUserId();
        String uri = servletRequestAttributes.getRequest().getRequestURI();
        String method = servletRequestAttributes.getRequest().getMethod();
        return "uri=%s httpMethod=%s userId=%s".formatted(uri, method, userId);
    }

    private String resolveLayer(Class<?> declaringClass) {
        String className = declaringClass.getName();
        if (className.contains(".controller.")) {
            return "controller";
        }
        if (declaringClass.isAnnotationPresent(RestController.class)) {
            return "controller";
        }
        if (declaringClass.isAnnotationPresent(Service.class)) {
            return "service";
        }
        return "service";
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private String truncate(String value) {
        if (value == null) {
            return "null";
        }
        int maxLength = Math.max(commonProperties.getLogging().getArgMaxLength(), 16);
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(" + value.length() + ")";
    }
}
