package edu.xjtlu.cpt202.backend.modules.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Source document stored in Redis specialist search index.
 *
 * @author QiranXiao
 * @since 2026/5/7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSpecialistSearchIndexDocument {

    private Long specialistId;
    private String specialistName;
    private String categoryName;
    private String level;
    private String bio;
    private String status;
    private String content;
}
