package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

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
}

