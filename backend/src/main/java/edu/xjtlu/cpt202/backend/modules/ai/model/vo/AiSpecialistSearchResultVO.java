package edu.xjtlu.cpt202.backend.modules.ai.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * AI-facing specialist search result payload.
 *
 * @author Codex
 * @since 2026/5/7
 */
@Data
@Builder
public class AiSpecialistSearchResultVO {

    private String query;
    private String appliedCategoryFilter;
    private String appliedLevelFilter;
    private String appliedNameFilter;
    private int returnedCount;
    private List<AiSpecialistSearchItemVO> items;
}
