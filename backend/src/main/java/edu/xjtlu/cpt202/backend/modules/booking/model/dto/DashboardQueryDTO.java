package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Dashboard statistics query DTO.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
@Data
@Schema(description = "Dashboard statistics query DTO for optional date range filtering")
public class DashboardQueryDTO {

    @Schema(description = "Start date, format: yyyy-MM-dd", example = "2026-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "End date, format: yyyy-MM-dd", example = "2026-04-14")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
