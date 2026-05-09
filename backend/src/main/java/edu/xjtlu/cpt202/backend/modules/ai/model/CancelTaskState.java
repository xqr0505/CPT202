package edu.xjtlu.cpt202.backend.modules.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Redis-backed state for the cancel booking workflow.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelTaskState implements Serializable {

    private Step step;

    private Long targetBookingId;

    private List<Long> candidateBookingIds;

    private String taskStateText;

    private String disambiguationHint;

    public enum Step {
        IDENTIFY,
        VALIDATE
    }
}
