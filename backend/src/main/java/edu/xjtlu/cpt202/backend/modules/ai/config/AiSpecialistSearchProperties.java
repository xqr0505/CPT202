package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Redis hybrid-search settings for AI specialist recommendation.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.search.specialist")
public class AiSpecialistSearchProperties {

    private boolean enabled = true;

    private String docPrefix = "expertlink:ai:specialist:";

    private String indexName = "expertlink:ai:specialist";

    @Min(1)
    private int topK = 5;

    @Min(1)
    private int maxResults = 3;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.90D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double strongKeywordSimilarityThreshold = 0.88D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double semanticWeight = 0.60D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double keywordWeight = 0.40D;

    private boolean includeInactive = false;
}
