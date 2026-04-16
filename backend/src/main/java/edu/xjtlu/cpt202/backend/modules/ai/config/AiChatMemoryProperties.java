package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configurable chat memory settings for the AI module.
 *
 * @author QiranXiao
 * @since 2026/4/15
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.chat.memory")
public class AiChatMemoryProperties {

    @Min(1)
    private Integer maxMessages = 20;

    @Min(1)
    private Long ttlSeconds = 86400L;

    @NotBlank
    private String keyPrefix = "expertlink:ai:memory";
}
