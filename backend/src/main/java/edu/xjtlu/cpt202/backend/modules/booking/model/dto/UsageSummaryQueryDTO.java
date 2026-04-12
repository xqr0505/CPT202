package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Usage summary query DTO.
 *
 * @author QiranXiao
 * @since 2026/4/12
 */
@Data
@Schema(description = "Usage summary query DTO for optional date range filtering")
public class UsageSummaryQueryDTO {

    @Schema(description = "Start date, format: yyyy-MM-dd", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "End date, format: yyyy-MM-dd", example = "2024-01-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
