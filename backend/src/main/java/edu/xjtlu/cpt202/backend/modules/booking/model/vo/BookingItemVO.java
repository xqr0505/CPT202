package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Common Booking Item VO for Lists")
public class BookingItemVO {

    @Schema(description = "Booking ID (String format for frontend)", example = "1001")
    private String id;

    @Schema(description = "Specialist ID (Needed if user wants to 'Book Again')", example = "101")
    private String specialistId;

    @Schema(description = "Specialist Name", example = "Dr. John Doe")
    private String specialistName;

    @Schema(description = "Specialist Avatar URL", example = "https://example.com/avatar.jpg")
    private String specialistAvatar;

    @Schema(description = "Booking Topic", example = "Mental Health Consultation")
    private String serviceName;

    @Schema(description = "Appointment Date and Time", example = "2026-04-20T10:00:00")
    private LocalDateTime appointmentDateTime;

    @Schema(description = "Booking Status: PENDING, CONFIRMED, COMPLETED, CANCELLED", example = "COMPLETED")
    private String status;

    @Schema(description = "Booking Amount (Optional for history)", example = "150.00")
    private BigDecimal amount;
}
