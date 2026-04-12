package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.modules.booking.config.BookingCustomerChangeConfig;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CustomerBookingChangePolicyServiceImplTest {

    private BookingCustomerChangeConfig props;
    private CustomerBookingChangePolicyServiceImpl policy;

    @BeforeEach
    void setUp() {
        props = new BookingCustomerChangeConfig();
        props.setFullRefundLeadHours(24);
        props.setMinLeadHours(2);
        props.setLateWindowPenaltyRatio(new BigDecimal("0.30"));
        policy = new CustomerBookingChangePolicyServiceImpl(props);
    }

    @Test
    void fullRefundWhenMoreThan24HoursRemaining() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        LocalDateTime start = now.plusHours(25);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.CONFIRMED.name(), start, now, new BigDecimal("100.00"));
        assertTrue(q.isAllowed());
        assertEquals("FULL_REFUND", q.getPolicyType());
        assertEquals(new BigDecimal("100.00"), q.getRefundAmount());
        assertEquals(new BigDecimal("0.00"), q.getPenaltyAmount());
    }

    @Test
    void lateWindowPenaltyBetween2And24Hours() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        LocalDateTime start = now.plusHours(10);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.PENDING.name(), start, now, new BigDecimal("100.00"));
        assertTrue(q.isAllowed());
        assertEquals("LATE_WINDOW_PENALTY", q.getPolicyType());
        assertEquals(new BigDecimal("70.00"), q.getRefundAmount());
        assertEquals(new BigDecimal("30.00"), q.getPenaltyAmount());
    }

    @Test
    void blockedWithin2Hours() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        LocalDateTime start = now.plusHours(2);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.CONFIRMED.name(), start, now, new BigDecimal("100.00"));
        assertFalse(q.isAllowed());
        assertEquals("TOO_CLOSE_TO_START", q.getReasonCode());
        assertEquals("BLOCKED", q.getPolicyType());
    }

    @Test
    void blockedJustAfter2HoursIsAllowedWithPenalty() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
        LocalDateTime start = now.plusHours(2).plusSeconds(1);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.CONFIRMED.name(), start, now, new BigDecimal("100.00"));
        assertTrue(q.isAllowed());
        assertEquals("LATE_WINDOW_PENALTY", q.getPolicyType());
    }

    @Test
    void invalidStatus() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        LocalDateTime start = now.plusDays(7);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.COMPLETED.name(), start, now, new BigDecimal("50.00"));
        assertFalse(q.isAllowed());
        assertEquals("INVALID_STATUS", q.getReasonCode());
    }

    @Test
    void slotAlreadyStarted() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0);
        LocalDateTime start = now.minusMinutes(5);
        BookingCancelQuoteVO q = policy.customerCancellationQuote(
                BookingStatusEnum.CONFIRMED.name(), start, now, new BigDecimal("50.00"));
        assertFalse(q.isAllowed());
        assertEquals("SLOT_ALREADY_STARTED", q.getReasonCode());
    }
}
