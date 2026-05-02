package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Lightweight intent router settings.
 *
 * @author QiranXiao
 * @since 2026/5/3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.intent.router")
public class AiIntentRouterProperties {

    @Min(100)
    private long timeoutMs = 1200L;
}
