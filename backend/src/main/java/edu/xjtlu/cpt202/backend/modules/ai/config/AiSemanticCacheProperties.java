package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Semantic cache settings for knowledge QA results.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.cache.qa")
public class AiSemanticCacheProperties {

    private boolean enabled = true;

    private String exactPrefix = "ai:cache:qa:exact:";

    private String docPrefix = "ai:cache:qa:";

    private String indexName = "expertlink:qa:cache";

    private boolean semanticEnabled = false;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.96D;

    @Min(1)
    private int maxResults = 1;

    @Min(1)
    private long ttlSeconds = 604800L;
}
