package edu.xjtlu.cpt202.backend.modules.booking.service;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer cancellation and reschedule policy calculation.
 */
public interface CustomerBookingChangePolicyService {

    /**
     * Cancel preview: Calculate and show the refund and penalty when the customer cancels the booking.
     *
     * @param bookingStatus 
     * @param slotStartAt   
     * @param now           
     * @param orderAmount   
     */
    BookingCancelQuoteVO customerCancellationQuote(
            String bookingStatus,
            LocalDateTime slotStartAt,
            LocalDateTime now,
            BigDecimal orderAmount
    );

    /**
     * Reschedule preview: Calculate and show the refund and penalty when the customer reschedules the booking.
     * If the specialist changed the consultation fee, the new price will be used to calculate show the difference.
     */
    BookingRescheduleQuoteVO customerRescheduleQuote(
            String bookingStatus,
            LocalDateTime currentBookingSlotStartAt,
            LocalDateTime now,
            BigDecimal originalPrice,
            BigDecimal newPrice
    );
}
