package edu.xjtlu.cpt202.backend.modules.ai.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * AI-facing specialist search result item.
 *
 * @author Codex
 * @since 2026/5/7
 */
@Data
@Builder
public class AiSpecialistSearchItemVO {

    private Long specialistId;
    private String specialistName;
    private String categoryName;
    private String level;
    private String matchSummary;
}
