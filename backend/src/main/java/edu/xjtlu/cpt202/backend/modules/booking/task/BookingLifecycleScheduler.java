package edu.xjtlu.cpt202.backend.modules.booking.task;

import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically advances booking lifecycle states that depend on wall-clock time.
 */
@Component
@RequiredArgsConstructor
public class BookingLifecycleScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.auto-complete.fixed-delay-ms:60000}")
    public void autoCompleteExpiredConfirmedBookings() {
        bookingService.autoCompleteExpiredConfirmedBookings();
    }
}
