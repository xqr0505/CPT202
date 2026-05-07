package edu.xjtlu.cpt202.backend.modules.ai.service;

/**
 * Maintains Redis specialist-search documents.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
public interface AiSpecialistSearchIndexService {

    void rebuildAll();

    void upsertSpecialist(Long specialistId);

    void deleteSpecialist(Long specialistId);
}
