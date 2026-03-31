package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author QiranXiao
 * @date 2026/3/31
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for querying booking history list")
public class BookingHistoryQueryDTO {

    @Schema(description = "Page number", example = "1", defaultValue = "1")
    @Builder.Default
    private Integer pageNo = 1;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer pageSize = 10;

    @Schema(description = "Time scope (UPCOMING or HISTORY) to filter bookings by time", example = "UPCOMING")
    private String timeScope;

    @Schema(description = "Booking status filter (e.g., COMPLETED, CANCELLED)", example = "COMPLETED")
    private String status;
}
