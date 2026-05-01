package edu.xjtlu.cpt202.backend.modules.ai.profiling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local trace context for AI chat profiling.
 *
 * @author OpenAI
 * @since 2026/5/1
 */
public final class AiChatTraceContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Long>> STAGE_COSTS_HOLDER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private AiChatTraceContext() {
    }

    public static String ensureTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    public static void restoreTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            TRACE_ID_HOLDER.remove();
            STAGE_COSTS_HOLDER.remove();
            return;
        }
        String current = TRACE_ID_HOLDER.get();
        if (traceId.equals(current)) {
            return;
        }
        TRACE_ID_HOLDER.set(traceId);
        // Only reset stage costs when switching to a different trace.
        STAGE_COSTS_HOLDER.set(new ConcurrentHashMap<>());
    }

    public static void recordStage(String stage, long costMs) {
        if (stage == null || stage.isBlank()) {
            return;
        }
        STAGE_COSTS_HOLDER.get().put(stage, costMs);
    }

    public static Map<String, Long> snapshotStages() {
        return new LinkedHashMap<>(STAGE_COSTS_HOLDER.get());
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
        STAGE_COSTS_HOLDER.remove();
    }
}
