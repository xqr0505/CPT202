package edu.xjtlu.cpt202.backend.modules.schedule.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * VO for time slot details.
 * @author Schedule Module Team
 */
@Data
public class TimeSlotVO {

    private Long id;

    private Long specialistId;

    private Long recurringRuleId;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String status;

    private Integer isDeleted;

    private String statusDesc;

    private Long bookingId;

    private String bookingStatus;

    private String bookingStatusDesc;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
