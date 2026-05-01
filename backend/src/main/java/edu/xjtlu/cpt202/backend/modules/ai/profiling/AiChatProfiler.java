package edu.xjtlu.cpt202.backend.modules.ai.profiling;

import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Central profiler logger for AI chat pipeline stages.
 *
 * @author QiranXiao
 * @since 2026/5/1
 */
@Slf4j
@Component
public class AiChatProfiler {

    private final CommonProperties commonProperties;

    public AiChatProfiler(CommonProperties commonProperties) {
        this.commonProperties = commonProperties;
    }

    public String startTrace(String stage, Long userId, String messagePreview) {
        if (!isEnabled()) {
            return null;
        }
        String traceId = AiChatTraceContext.ensureTraceId();
        log.info("aiChat traceId={} stage={} userId={} messagePreview={}", traceId, stage, userId, messagePreview);
        return traceId;
    }

    public void logStage(String stage, long costMs, Map<String, ?> fields) {
        if (!isEnabled()) {
            return;
        }
        AiChatTraceContext.recordStage(stage, costMs);
        log.info("aiChat traceId={} stage={} costMs={} details={}",
                AiChatTraceContext.ensureTraceId(),
                stage,
                costMs,
                fields == null ? Map.of() : fields
        );
    }

    public void logEvent(String stage, Map<String, ?> fields) {
        if (!isEnabled()) {
            return;
        }
        log.info("aiChat traceId={} stage={} details={}",
                AiChatTraceContext.ensureTraceId(),
                stage,
                fields == null ? Map.of() : fields
        );
    }

    public void logSummary(String stage, long totalMs, Map<String, ?> fields) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Long> recordedStages = AiChatTraceContext.snapshotStages();
        log.info("aiChat traceId={} stage={} totalMs={} stageCosts={} details={}",
                AiChatTraceContext.ensureTraceId(),
                stage,
                totalMs,
                recordedStages,
                fields == null ? Map.of() : fields
        );
    }

    public void clearTrace() {
        AiChatTraceContext.clear();
    }

    private boolean isEnabled() {
        return commonProperties.getLogging().isAiChatProfilingEnabled();
    }
}
