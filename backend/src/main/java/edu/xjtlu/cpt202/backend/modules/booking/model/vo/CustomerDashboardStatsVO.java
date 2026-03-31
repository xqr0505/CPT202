package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/3/31
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer dashboard statistics view object")
public class CustomerDashboardStatsVO {

    @Schema(description = "Total amount spent", example = "1200.50")
    private BigDecimal totalAmount;

    @Schema(description = "Total duration of consultations in minutes", example = "300")
    private Integer totalDuration;

    @Schema(description = "Total number of completed consultations", example = "5")
    private Integer totalCompleted;

    @Schema(description = "List of avatars of consulted specialists")
    private List<String> specialistAvatars;
}

