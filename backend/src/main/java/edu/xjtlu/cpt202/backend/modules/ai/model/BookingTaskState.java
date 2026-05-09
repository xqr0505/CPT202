package edu.xjtlu.cpt202.backend.modules.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Redis-backed state for the booking workflow.
 *
 * @author QiranXiao
 * @since 2026/5/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTaskState implements Serializable {

    private String preferredTopic;

    private String customerNotes;

    private List<CandidateSlotState> candidateSlots;

    private String taskStateText;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateSlotState implements Serializable {
        private Long specialistId;
        private String specialistName;
        private BigDecimal consultationFee;
        private Long slotId;
        private String slotDate;
        private String startTime;
        private String endTime;
    }
}
