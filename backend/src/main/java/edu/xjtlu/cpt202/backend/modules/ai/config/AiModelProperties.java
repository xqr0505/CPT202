package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Output and behavior controls for chat model invocations.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.model")
public class AiModelProperties {

    @Min(1)
    private Integer maxOutputTokens = 512;
}
