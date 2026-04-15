package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer reschedule quote: price snapshot vs current specialist fee, plus time-window penalty.
 * Per-slot price is not modeled; new price comes from current {@code specialist_profiles.consultation_fee}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer reschedule quote (preview)")
public class BookingRescheduleQuoteVO {

    @Schema(description = "whether to allow entering the confirmation reschedule process")
    private boolean allowed;

    @Schema(description = "Reason code when not allowed", example = "TOO_CLOSE_TO_START")
    private String reasonCode;

    @Schema(description = "message for display")
    private String message;

    @Schema(description = "policy type", example = "FULL_REFUND")
    private String policyType;

    @Schema(description = "current booking slot start time")
    private LocalDateTime bookingStartAt;

    @Schema(description = "Price locked on the booking at purchase time")
    private BigDecimal originalPrice;

    @Schema(description = "Current specialist consultation fee (applied when specialist changes the consultation fee)")
    private BigDecimal newPrice;

    @Schema(description = "defference: newPrice - originalPrice")
    private BigDecimal priceDifference;

    @Schema(description = "Time-window penalty on the original order amount (same rule as cancellation)")
    private BigDecimal penaltyAmount;

    @Schema(description = "Refund when new price is lower than original: max(0, originalPrice - newPrice)")
    private BigDecimal refundAmount;

    @Schema(description = "Amount to collect: max(0, priceDifference) + penaltyAmount")
    private BigDecimal payableAmount;
}
