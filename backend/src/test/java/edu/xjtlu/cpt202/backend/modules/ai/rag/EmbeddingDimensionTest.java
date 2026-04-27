package edu.xjtlu.cpt202.backend.modules.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;

class EmbeddingDimensionTest {

    @Test
    void verifyEmbeddingDimension() {
        // Avoid @SpringBootTest here: bootstrapping the full app context triggers DB/Flyway connections.
        String apiKey = getEnvOrDotenv("DASHSCOPE_API_KEY");
        String modelName = getEnvOrDotenv("DASHSCOPE_EMBEDDING_MODEL_NAME");
        if (modelName == null || modelName.isBlank()) {
            modelName = "text-embedding-v2";
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY is required (env or .env).");
        }

        EmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();

        String text = "test embedding dimension";
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();

        System.out.println("model: " + modelName);
        System.out.println("implementation: " + embeddingModel.getClass().getName());
        System.out.println("embedding dimension: " + (vector == null ? "null" : vector.length));
    }

    private static String getEnvOrDotenv(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        // Fallback: parse ../.env (repo root) or .env (backend root), without bringing in extra deps.
        value = readDotenvValue("../.env", key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return readDotenvValue(".env", key);
    }

    private static String readDotenvValue(String path, String key) {
        try {
            java.nio.file.Path file = java.nio.file.Path.of(path);
            if (!java.nio.file.Files.exists(file)) {
                return null;
            }
            for (String line : java.nio.file.Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String k = trimmed.substring(0, idx).trim();
                if (!k.equals(key)) {
                    continue;
                }
                String v = trimmed.substring(idx + 1).trim();
                // Strip optional surrounding quotes.
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                return v;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
