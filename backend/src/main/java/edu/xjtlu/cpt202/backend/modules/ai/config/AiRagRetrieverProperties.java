package edu.xjtlu.cpt202.backend.modules.ai.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Request-level budget for RAG retrieval tool calls.
 *
 * @author QiranXiao
 * @since 2026/4/26
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ai.rag.retriever")
public class AiRagRetrieverProperties {

    @Min(100)
    private Long timeoutMs = 3000L;
}
