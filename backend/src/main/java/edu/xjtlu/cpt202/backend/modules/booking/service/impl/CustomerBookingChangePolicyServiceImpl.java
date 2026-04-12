package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.modules.booking.config.BookingCustomerChangeConfig;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.CustomerBookingChangePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Customer cancellation rules:
 * >24 hours to start: full refund;
 * >2 hours and <=24 hours to start: penalty applied;
 * <=2 hours to start: cannot cancel or reschedule;
 * only PENDING or CONFIRMED can be changed.
 */
@Service
@RequiredArgsConstructor
public class CustomerBookingChangePolicyServiceImpl implements CustomerBookingChangePolicyService {

    private static final int MONEY_SCALE = 2;

    private final BookingCustomerChangeConfig props;

    @Override
    public BookingCancelQuoteVO customerCancellationQuote(
            String bookingStatus,
            LocalDateTime slotStartAt,
            LocalDateTime now,
            BigDecimal orderAmount) {

        BigDecimal base = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        base = base.max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (!isCustomerCancellableStatus(bookingStatus)) {
            return BookingCancelQuoteVO.builder()
                    .allowed(false)
                    .orderAmount(base)
                    .penaltyAmount(zeroMoney())
                    .refundAmount(zeroMoney())
                    .reasonCode("INVALID_STATUS")
                    .message("Booking is not cancellable")
                    .policyType("NOT_CANCELLABLE")
                    .bookingStartAt(slotStartAt)
                    .build();
        }

        if (slotStartAt == null || now == null) {
            return BookingCancelQuoteVO.builder()
                    .allowed(false)
                    .orderAmount(base)
                    .penaltyAmount(zeroMoney())
                    .refundAmount(zeroMoney())
                    .reasonCode("SLOT_TIME_MISSING")
                    .message("Cannot parse booking start time")
                    .policyType("BLOCKED")
                    .bookingStartAt(slotStartAt)
                    .build();
        }

        Duration remaining = Duration.between(now, slotStartAt);
        Duration minLead = Duration.ofHours(props.getMinLeadHours());
        Duration fullRefundLead = Duration.ofHours(props.getFullRefundLeadHours());

        // Java 8 无 Duration.isPositive()；<=0 表示已开始或已过（含剩余时间为 0）
        if (remaining.compareTo(Duration.ZERO) <= 0) {
            return BookingCancelQuoteVO.builder()
                    .allowed(false)
                    .orderAmount(base)
                    .penaltyAmount(zeroMoney())
                    .refundAmount(zeroMoney())
                    .reasonCode("SLOT_ALREADY_STARTED")
                    .message("Slot already started, cannot cancel or reschedule")
                    .policyType("BLOCKED")
                    .bookingStartAt(slotStartAt)
                    .build();
        }

        if (remaining.compareTo(minLead) <= 0) {
            return BookingCancelQuoteVO.builder()
                    .allowed(false)
                    .reasonCode("TOO_CLOSE_TO_START")
                    .message("Less than " + props.getMinLeadHours() + " hours to start, cannot cancel or reschedule")
                    .policyType("BLOCKED")
                    .bookingStartAt(slotStartAt)
                    .orderAmount(base)
                    .refundAmount(zeroMoney())
                    .penaltyAmount(zeroMoney())
                    .build();
        }

        if (remaining.compareTo(fullRefundLead) > 0) {
            return BookingCancelQuoteVO.builder()
                    .allowed(true)
                    .reasonCode(null)
                    .message("More than " + props.getFullRefundLeadHours() + " hours to start, full refund")
                    .policyType("FULL_REFUND")
                    .bookingStartAt(slotStartAt)
                    .orderAmount(base)
                    .refundAmount(base)
                    .penaltyAmount(zeroMoney())
                    .build();
        }

        BigDecimal ratio = props.getPenaltyRatio();
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) < 0) {
            ratio = BigDecimal.ZERO;
        }
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }

        BigDecimal penalty = base.multiply(ratio).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (penalty.compareTo(base) > 0) {
            penalty = base;
        }
        BigDecimal refund = base.subtract(penalty).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (refund.compareTo(BigDecimal.ZERO) < 0) {
            refund = zeroMoney();
        }

        return BookingCancelQuoteVO.builder()
                .allowed(true)
                .reasonCode(null)
                .message("Between " + props.getMinLeadHours() + " and " + props.getFullRefundLeadHours()
                        + " hours to start, penalty applied")
                .policyType("LATE_WINDOW_PENALTY")
                .bookingStartAt(slotStartAt)
                .orderAmount(base)
                .refundAmount(refund)
                .penaltyAmount(penalty)
                .build();
    }

    private static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isCustomerCancellableStatus(String bookingStatus) {
        if (bookingStatus == null) {
            return false;
        }
        return BookingStatusEnum.PENDING.name().equals(bookingStatus)
                || BookingStatusEnum.CONFIRMED.name().equals(bookingStatus);
    }
}
