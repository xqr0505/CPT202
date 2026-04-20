package edu.xjtlu.cpt202.backend.modules.ai.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope/Qwen embedding model configuration.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Data
@ConfigurationProperties(prefix = "ai.embedding.dashscope")
public class DashScopeEmbeddingProperties {

    private String apiKey;
    private String modelName = "text-embedding-v4";
    private int dimension = 1536;
}
