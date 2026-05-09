package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Result payload for AI booking auto-submit tool.
 *
 * @author QiranXiao
 * @since 2026/4/18
 */
@Data
@Builder
public class AiBookingAutoSubmitResultVO {

    private boolean success;

    private boolean readyToSubmit;

    private String message;

    private Long bookingId;

    private String bookingStatus;

    private Long specialistId;

    private Long slotId;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String specialistName;

    private BigDecimal consultationFee;

    private String topic;

    private String customerNotes;

    private List<String> availableTopics;

    private List<String> warnings;

    private List<AvailableSlotVO> availableSlots;

    @Data
    @Builder
    public static class AvailableSlotVO {
        private Long slotId;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
