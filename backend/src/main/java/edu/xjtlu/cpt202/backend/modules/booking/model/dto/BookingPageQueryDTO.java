package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author QiranXiao
 * @since 2026/4/4
 */
@Data
@Schema(description = "Booking List Pagination Query DTO")
public class BookingPageQueryDTO {

    @Schema(description = "Page number", example = "1", defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "Page size (10 per page as per PBI)", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "Tab type: 'UPCOMING' or 'HISTORY'. Optional.", example = "HISTORY")
    private String tab;

    @Schema(description = "Specific status filter: 'COMPLETED', 'CANCELLED', 'PENDING'. Null means 'All'", example = "COMPLETED")
    private String status;
}
