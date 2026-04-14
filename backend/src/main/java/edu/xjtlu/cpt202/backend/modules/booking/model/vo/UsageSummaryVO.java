package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Usage summary VO for customer dashboard.
 *
 * @author QiranXiao
 * @since 2026/4/12
 */
@Data
@Schema(description = "Customer usage summary view object")
public class UsageSummaryVO {

    @Schema(description = "Total completed appointments", example = "12")
    private Integer totalCompletedAppointments = 0;

    @Schema(description = "Total amount spent", example = "1680.00")
    private BigDecimal totalAmountSpent = BigDecimal.ZERO;

    @Schema(description = "Total consultation hours", example = "6.5")
    private Double totalConsultationHours = 0.0D;

    @Schema(description = "Distinct specialists consulted by current customer")
    private List<ConsultedExpertVO> consultedExperts = new ArrayList<>();

    @Data
    @Schema(description = "Consulted specialist item")
    public static class ConsultedExpertVO {

        @Schema(description = "Specialist ID", example = "1001")
        private Long specialistId;

        @Schema(description = "Specialist full name", example = "Dr. Emily Chen")
        private String specialistName;
    }
}
