package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingAutoSubmitResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * AI tool for preparing booking confirmation drafts based on user input and current availability.
 * @author QiranXiao
 * @since 2026/4/18
 */
@Component
public class AiBookingSubmitTool {

    private final SpecialistQueryService specialistQueryService;
    private final BookingService bookingService;

    public AiBookingSubmitTool(SpecialistQueryService specialistQueryService, BookingService bookingService) {
        this.specialistQueryService = specialistQueryService;
        this.bookingService = bookingService;
    }

    @Tool("""
            Prepare a booking confirmation draft for the current logged-in customer.
            Use this tool when the customer explicitly asks to place a booking now.
            You must provide specialistId, booking date, and at least startTime.
            If endTime is provided, this tool matches the exact slot range.
            Do not assume the order is created yet; this draft must be confirmed in UI before final submission.
            """)
    public AiBookingAutoSubmitResultVO submitCurrentCustomerBooking(
            @ToolMemoryId Long customerId,
            @P("Specialist profile ID, for example 1. Required.") Long specialistId,
            @P("Booking date in yyyy-MM-dd, for example 2026-04-20. Required.") LocalDate slotDate,
            @P("Slot start time in HH:mm or HH:mm:ss, for example 10:30 or 10:30:00. Required.") LocalTime startTime,
            @P("Slot end time in HH:mm or HH:mm:ss, for example 11:00 or 11:00:00. Optional.") LocalTime endTime,
            @P("Preferred topic from active booking topics. Optional.") String preferredTopic,
            @P("Optional customer notes.") String customerNotes
    ) {
        if (customerId == null) {
            return failure("Customer identity is missing. Please login again.", specialistId, null, slotDate, startTime, endTime);
        }
        if (specialistId == null || specialistId <= 0) {
            return failure("specialistId is required and must be positive.", specialistId, null, slotDate, startTime, endTime);
        }
        if (slotDate == null) {
            return failure("slotDate is required.", specialistId, null, null, startTime, endTime);
        }
        if (startTime == null) {
            return failure("startTime is required.", specialistId, null, slotDate, null, endTime);
        }

        List<SpecialistAvailabilityVO> availability = specialistQueryService.listAvailability(specialistId, slotDate);
        List<SpecialistAvailabilityVO> availableSlots = availability.stream()
                .filter(slot -> TimeSlotStatusEnum.AVAILABLE.name().equals(slot.getStatus()))
                .toList();

        SpecialistAvailabilityVO matchedSlot = availableSlots.stream()
                .filter(slot -> isMatched(slot, startTime, endTime))
                .findFirst()
                .orElse(null);
        if (matchedSlot == null) {
            return AiBookingAutoSubmitResultVO.builder()
                    .success(false)
                    .readyToSubmit(false)
                    .message("Requested slot is not available. Please choose one of availableSlots.")
                    .specialistId(specialistId)
                    .slotDate(slotDate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .availableSlots(toAvailableSlotVOs(availableSlots))
                    .build();
        }

        AiBookingFormDraftVO draft = bookingService.buildAiBookingDraft(
                customerId,
                specialistId,
                matchedSlot.getId(),
                preferredTopic,
                customerNotes
        );
        if (draft.getTopic() == null || draft.getTopic().isBlank()) {
            return AiBookingAutoSubmitResultVO.builder()
                    .success(false)
                    .readyToSubmit(false)
                    .message("No valid booking topic is available. Please contact support.")
                    .specialistId(specialistId)
                    .slotId(matchedSlot.getId())
                    .slotDate(slotDate)
                    .startTime(matchedSlot.getStartTime())
                    .endTime(matchedSlot.getEndTime())
                    .warnings(draft.getWarnings())
                    .availableSlots(toAvailableSlotVOs(availableSlots))
                    .build();
        }

        SpecialistDetailVO specialistDetail = specialistQueryService.getSpecialistDetail(specialistId);
        return AiBookingAutoSubmitResultVO.builder()
                .success(true)
                .readyToSubmit(true)
                .message("Booking draft prepared. Please confirm submission in UI.")
                .specialistId(specialistId)
                .slotId(matchedSlot.getId())
                .slotDate(slotDate)
                .startTime(matchedSlot.getStartTime())
                .endTime(matchedSlot.getEndTime())
                .specialistName(specialistDetail.getName())
                .consultationFee(specialistDetail.getConsultationFee())
                .topic(draft.getTopic())
                .customerNotes(draft.getCustomerNotes())
                .warnings(draft.getWarnings())
                .build();
    }

    private boolean isMatched(SpecialistAvailabilityVO slot, LocalTime startTime, LocalTime endTime) {
        if (!Objects.equals(slot.getStartTime(), startTime)) {
            return false;
        }
        if (endTime == null) {
            return true;
        }
        return Objects.equals(slot.getEndTime(), endTime);
    }

    private List<AiBookingAutoSubmitResultVO.AvailableSlotVO> toAvailableSlotVOs(List<SpecialistAvailabilityVO> slots) {
        return slots.stream()
                .map(slot -> AiBookingAutoSubmitResultVO.AvailableSlotVO.builder()
                        .slotId(slot.getId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build())
                .toList();
    }

    private AiBookingAutoSubmitResultVO failure(
            String message,
            Long specialistId,
            Long slotId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return AiBookingAutoSubmitResultVO.builder()
                .success(false)
                .readyToSubmit(false)
                .message(message)
                .specialistId(specialistId)
                .slotId(slotId)
                .slotDate(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}
