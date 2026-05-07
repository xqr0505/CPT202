package edu.xjtlu.cpt202.backend.modules.ai.service;

import edu.xjtlu.cpt202.backend.modules.ai.model.vo.AiSpecialistSearchResultVO;

/**
 * Hybrid specialist search for AI tooling.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
public interface AiSpecialistSearchService {

    AiSpecialistSearchResultVO search(String searchQuery, String categoryFilter, String levelFilter, String nameFilter);
}
