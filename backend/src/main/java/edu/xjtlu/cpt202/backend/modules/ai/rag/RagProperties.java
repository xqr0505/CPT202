package edu.xjtlu.cpt202.backend.modules.ai.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the knowledge-base RAG pipeline.
 *
 * @author QiranXiao
 * @since 2026/4/21
 */
@Data
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    private boolean enabled = true;
    private String knowledgeLocation = "classpath*:/knowledge/**/*.md";
    private Redis redis = new Redis();
    private int maxResults = 3;
    private double minScore = 0.7;

    @Data
    public static class Redis {
        private String host = "127.0.0.1";
        private int port = 6379;
        private String username;
        private String password;
        private String indexName = "expertlink:rag:knowledge";
        private String prefix = "expertlink:rag:segment:";
    }
}
