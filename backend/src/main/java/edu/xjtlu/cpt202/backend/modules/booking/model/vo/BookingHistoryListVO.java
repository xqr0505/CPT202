package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author QiranXiao
 * @date 2026/3/31
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking history list card view object")
public class BookingHistoryListVO {

    @Schema(description = "Booking ID", example = "10")
    private Long id;

    @Schema(description = "Specialist name", example = "Dr. Jane Smith")
    private String specialistName;

    @Schema(description = "Specialist title/profession", example = "Consultant")
    private String specialistTitle;

    @Schema(description = "Specialist avatar URL", example = "https://example.com/avatar_jane.jpg")
    private String specialistAvatar;

    @Schema(description = "Consultation date and start time", example = "2024-05-15T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Consultation end time", example = "2024-05-15T09:45:00")
    private LocalDateTime endTime;

    @Schema(description = "Consultation duration in minutes", example = "45")
    private Integer duration;

    @Schema(description = "Booking Status", example = "COMPLETED")
    private String status;

    @Schema(description = "Total cost of the booking", example = "150.00")
    private BigDecimal amount;
}

