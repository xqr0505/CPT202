package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customer/dashboard")
@RequiredArgsConstructor
@Tag(name = "Customer Dashboard", description = "Customer dashboard API endpoints")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming bookings", description = "Fetch the next 3 confirmed upcoming bookings for the current customer.")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<UpcomingBookingVO>> getUpcomingBookings() {
        Long customerId = SecurityUtils.getCurrentUserId();
        List<UpcomingBookingVO> upcomingBookings = bookingService.getUpcomingBookingsByCustomer(customerId, 3);
        return Result.success(upcomingBookings);
    }
}

