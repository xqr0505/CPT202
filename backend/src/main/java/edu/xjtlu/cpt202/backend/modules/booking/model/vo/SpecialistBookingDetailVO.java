package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpecialistBookingDetailVO {

    private Long id;

    private String customerName;

    private LocalDateTime requestedStartTime;

    private LocalDateTime requestedEndTime;

    private String topic;

    private LocalDateTime submissionTime;

    private String customerNotes;

    private String status;

    private LocalDateTime decisionTime;

    private String rejectionReason;
}
