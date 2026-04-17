package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * VO for booking detail information.
 * Contains specialist info, exact date/time, fees, status, and customer notes.
 *
 * @author QiranXiao
 * @since 2026/4/8
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking Detail View Object")
public class BookingDetailVO {

    @Schema(description = "Booking ID", example = "1001")
    private Long bookingId;

    @Schema(description = "Booking Status: PENDING, CONFIRMED, COMPLETED, CANCELLED", example = "CONFIRMED")
    private String status;

    // ========== Specialist Information ==========

    @Schema(description = "Specialist ID", example = "101")
    private Long specialistId;

    @Schema(description = "Specialist Name", example = "Dr. John Smith")
    private String specialistName;

    @Schema(description = "Specialist Avatar URL", example = "https://example.com/avatar.jpg")
    private String specialistAvatar;

    // ========== Time Information (AC2: exact date and time slot) ==========

    @Schema(description = "Slot Date in format YYYY-MM-DD", example = "2026-04-15")
    private String slotDate;

    @Schema(description = "Start Time in format HH:mm", example = "10:00")
    private String startTime;

    @Schema(description = "End Time in format HH:mm", example = "11:00")
    private String endTime;

    // ========== Order Information (AC2: total consultation fee) ==========

    @Schema(description = "Consultation Fee", example = "150.00")
    private BigDecimal price;

    // ========== User Submitted Information (AC2: submitted notes) ==========

    @Schema(description = "Consultation Topic", example = "Mental Health Consultation")
    private String topic;

    @Schema(description = "Customer Notes", example = "Please discuss about stress management")
    private String customerNotes;
}

