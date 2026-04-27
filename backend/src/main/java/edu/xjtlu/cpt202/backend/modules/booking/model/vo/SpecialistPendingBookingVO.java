package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Pending booking request item for specialist side.
 */
@Data
public class SpecialistPendingBookingVO {

    private Long id;

    private String customerName;

    private LocalDateTime requestedStartTime;

    private LocalDateTime requestedEndTime;

    private String topic;

    private LocalDateTime submissionTime;

    private LocalDateTime autoRejectAt;
}
