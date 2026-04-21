package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialist/booking-requests")
@Tag(name = "Specialist Booking Requests", description = "Specialist booking request API endpoints")
public class SpecialistBookingController {

    private final BookingService bookingService;

    public SpecialistBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending booking requests for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<List<SpecialistPendingBookingVO>> getPendingRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(bookingService.listPendingRequestsForSpecialist(currentUserId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get handled booking requests for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<List<SpecialistHandledBookingVO>> getHandledRequests() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(bookingService.listHandledRequestsForSpecialist(currentUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking request detail for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<SpecialistBookingDetailVO> getBookingRequestDetail(@PathVariable("id") Long bookingId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(bookingService.getBookingRequestDetailForSpecialist(bookingId, currentUserId));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending booking request")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<Void> approveBookingRequest(@PathVariable("id") Long bookingId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        bookingService.approveBookingRequest(bookingId, currentUserId);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending booking request")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<Void> rejectBookingRequest(
            @PathVariable("id") Long bookingId,
            @Valid @RequestBody SpecialistRejectBookingRequestDTO requestDTO) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        bookingService.rejectBookingRequest(bookingId, currentUserId, requestDTO);
        return Result.success();
    }

    @PostMapping("/{id}/force-cancel")
    @Operation(summary = "Force cancel a booking by specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public Result<Void> specialistForceCancelBooking(
            @PathVariable("id") Long bookingId,
            @Valid @RequestBody SpecialistForceCancelBookingRequestDTO requestDTO) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        bookingService.specialistForceCancelBooking(bookingId, currentUserId, requestDTO);
        return Result.success();
    }
}