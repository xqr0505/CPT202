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
@Schema(description = "Customer cancellation and reschedule policy calculation")
public class BookingCancelQuoteVO {

    @Schema(description = "whether to allow entering the confirmation cancellation process")
    private boolean allowed;

    @Schema(description = "reason code", example = "TOO_CLOSE_TO_START")
    private String reasonCode;

    @Schema(description = "message for display")
    private String message;

    @Schema(description = "policy type", example = "FULL_REFUND")
    private String policyType;

    @Schema(description = "booking start time")
    private LocalDateTime bookingStartAt;

    @Schema(description = "order amount")
    private BigDecimal orderAmount;

    @Schema(description = "estimated refund amount")
    private BigDecimal refundAmount;

    @Schema(description = "estimated penalty amount")
    private BigDecimal penaltyAmount;
}
