package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author QiranXiao
 * @since 2026/3/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Upcoming booking view object for dashboard")
public class UpcomingBookingVO {

    @Schema(description = "Booking ID", example = "1")
    private Long id;

    @Schema(description = "Specialist name", example = "Dr. John Doe")
    private String specialistName;

    @Schema(description = "Service name", example = "Mental Health Consultation")
    private String serviceName;

    @Schema(description = "Start time of the consultation", example = "2024-05-20T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Whether the appointment is today", example = "true")
    private Boolean today;

    @Schema(description = "Booking Status", example = "CONFIRMED")
    private String status;

}
