package edu.xjtlu.cpt202.backend.modules.ai.service;

import java.util.Optional;

/**
 * Semantic cache for knowledge QA results.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
public interface AiSemanticCacheService {

    Optional<CacheHit> get(String query, AiIntent intent);

    void putAsync(String query, String answer, AiIntent intent);

    void clearAll();

    record CacheHit(
            String cacheId,
            String answer,
            boolean exactMatch,
            double score
    ) {
    }
}
