package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Rewrite configuration for RAG query expansion.
 *
 * @author QiranXiao
 * @since 2026/4/26
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.rag.rewrite")
public class AiRagRewriteProperties {

    private boolean enabled = true;

    /**
     * Supported values: light_model, rule, off.
     */
    private String mode = "light_model";

    @Min(100)
    private Long timeoutMs = 1500L;

    @Min(1)
    private Integer maxQueries = 2;

    /**
     * Optional dedicated lightweight model for rewrite requests.
     * Falls back to main chat model when blank.
     */
    private String lightModelName = "qwen-turbo";

    public RewriteMode modeEnum() {
        return RewriteMode.from(mode);
    }

    public enum RewriteMode {
        LIGHT_MODEL,
        RULE,
        OFF;

        public static RewriteMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return LIGHT_MODEL;
            }
            String normalized = raw.trim().toUpperCase().replace('-', '_');
            return switch (normalized) {
                case "RULE" -> RULE;
                case "OFF" -> OFF;
                default -> LIGHT_MODEL;
            };
        }
    }
}
