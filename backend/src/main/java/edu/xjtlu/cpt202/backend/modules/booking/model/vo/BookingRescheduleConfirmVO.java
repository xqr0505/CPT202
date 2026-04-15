package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer reschedule confirm result")
public class BookingRescheduleConfirmVO {

    @Schema(description = "booking id")
    private Long bookingId;

    @Schema(description = "booking status after confirm", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "policy type", example = "FULL_REFUND")
    private String policyType;

    @Schema(description = "price difference: newPrice - originalPrice")
    private BigDecimal priceDifference;

    @Schema(description = "time-window penalty on original booking amount")
    private BigDecimal penaltyAmount;

    @Schema(description = "refund when new price is lower than original")
    private BigDecimal refundAmount;

    @Schema(description = "amount to collect: max(0, priceDifference) + penaltyAmount")
    private BigDecimal payableAmount;

    @Schema(description = "message for display")
    private String message;
}
