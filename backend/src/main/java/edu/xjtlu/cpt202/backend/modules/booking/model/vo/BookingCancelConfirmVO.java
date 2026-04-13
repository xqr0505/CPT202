package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Customer cancellation confirm result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer cancellation confirm result")
public class BookingCancelConfirmVO {

    @Schema(description = "booking id")
    private Long bookingId;

    @Schema(description = "new booking status", example = "CANCELLED")
    private String bookingStatus;

    @Schema(description = "applied policy type", example = "FULL_REFUND")
    private String policyType;

    @Schema(description = "refund amount")
    private BigDecimal refundAmount;

    @Schema(description = "penalty amount")
    private BigDecimal penaltyAmount;

    @Schema(description = "display message")
    private String message;
}
