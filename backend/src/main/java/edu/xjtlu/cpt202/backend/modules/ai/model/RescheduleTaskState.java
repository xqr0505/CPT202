package edu.xjtlu.cpt202.backend.modules.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Redis-backed state for the reschedule booking workflow.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleTaskState implements Serializable {

    private Step step;

    private Long targetBookingId;

    private String targetDate;

    private Long suggestedSlotId;

    private List<Long> candidateBookingIds;

    private LocalDate lookupStartDate;

    private LocalDate lookupEndDate;

    private String lookupTimeRangeType;

    private String targetTime;

    private String timeHint;

    private String taskStateText;

    private String disambiguationHint;

    public enum Step {
        IDENTIFY,
        PRE_CHECK,
        DONE
    }
}
