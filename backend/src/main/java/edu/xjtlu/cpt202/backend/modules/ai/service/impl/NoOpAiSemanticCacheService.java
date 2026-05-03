package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiSemanticCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * No-op fallback when semantic cache infrastructure is unavailable.
 *
 * @author QiranXiao
 * @since 2026/5/2
 */
@Service
@ConditionalOnMissingBean(AiSemanticCacheService.class)
public class NoOpAiSemanticCacheService implements AiSemanticCacheService {

    @Override
    public Optional<CacheHit> get(String query, AiIntent intent) {
        return Optional.empty();
    }

    @Override
    public void putAsync(String query, String answer, AiIntent intent) {
    }

    @Override
    public void clearAll() {
    }
}
