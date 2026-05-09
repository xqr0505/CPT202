package edu.xjtlu.cpt202.backend.modules.booking.service;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CustomerBookingChangePolicyService {

    BookingCancelQuoteVO customerCancellationQuote(
            String bookingStatus,
            LocalDateTime slotStartAt,
            LocalDateTime now,
            BigDecimal orderAmount
    );

    BookingRescheduleQuoteVO customerRescheduleQuote(
            String bookingStatus,
            LocalDateTime currentBookingSlotStartAt,
            LocalDateTime now,
            BigDecimal originalPrice,
            BigDecimal newPrice
    );
}
