package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialist/booking-requests")
@RequiredArgsConstructor
@Tag(name = "Specialist Booking Requests", description = "Specialist booking request API endpoints")
public class SpecialistBookingController {

    private final BookingService bookingService;

    @GetMapping("/pending")
    @Operation(summary = "Get pending booking requests for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<List<SpecialistPendingBookingVO>> getPendingRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(bookingService.listPendingRequestsForSpecialist(currentUserId));
    }
}