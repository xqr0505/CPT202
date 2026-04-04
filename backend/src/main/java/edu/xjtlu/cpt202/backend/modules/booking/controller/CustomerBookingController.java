package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
@Tag(name = "Customer Booking", description = "Customer booking API endpoints")
public class CustomerBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get cooking history", description = "Get paginated list of bookings based on time scope and status.")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<PageResult<BookingHistoryListVO>> getBookings(@ModelAttribute BookingHistoryQueryDTO queryDTO) {
        Long customerId = SecurityUtils.getCurrentUserId();
        PageResult<BookingHistoryListVO> result = bookingService.listBookings(customerId, queryDTO);
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a booking for a specialist time slot.")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<BookingCreateVO> createBooking(@Valid @RequestBody BookingCreateDTO createDTO) {
        Long customerId = SecurityUtils.getCurrentUserId();
        BookingCreateVO result = bookingService.createBooking(customerId, createDTO);
        return Result.success(result);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking detail", description = "Get detailed information for a specific booking.")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<BookingDetailVO> getBookingDetail(
            @Parameter(description = "Booking ID") @PathVariable Long bookingId) {
        Long customerId = SecurityUtils.getCurrentUserId();
        BookingDetailVO detail = bookingService.getBookingDetail(bookingId, customerId);
        return Result.success(detail);
    }
}
