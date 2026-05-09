package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @Schema(description = "Specialist ID", example = "101")
    private Long specialistId;

    @Schema(description = "Specialist Name", example = "Dr. John Smith")
    private String specialistName;

    @Schema(description = "Specialist Avatar URL", example = "https://example.com/avatar.jpg")
    private String specialistAvatar;

    @Schema(description = "Slot Date in format YYYY-MM-DD", example = "2026-04-15")
    private String slotDate;

    @Schema(description = "Start Time in format HH:mm", example = "10:00")
    private String startTime;

    @Schema(description = "End Time in format HH:mm", example = "11:00")
    private String endTime;

    @Schema(description = "Consultation Fee", example = "150.00")
    private BigDecimal price;

    @Schema(description = "Consultation Topic", example = "Mental Health Consultation")
    private String topic;

    @Schema(description = "Customer Notes", example = "Please discuss about stress management")
    private String customerNotes;
}

