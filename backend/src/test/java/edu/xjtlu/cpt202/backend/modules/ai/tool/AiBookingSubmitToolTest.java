package edu.xjtlu.cpt202.backend.modules.ai.tool;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingAutoSubmitResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiBookingSubmitToolTest {

    @Test
    void shouldPrepareBookingConfirmationWhenMatchedSlotExists() {
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        AiBookingSubmitTool tool = new AiBookingSubmitTool(specialistQueryService, bookingService);

        SpecialistAvailabilityVO slot = new SpecialistAvailabilityVO();
        slot.setId(88L);
        slot.setSlotDate(LocalDate.of(2026, 4, 20));
        slot.setStartTime(LocalTime.of(10, 30));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus("AVAILABLE");

        when(specialistQueryService.listAvailability(1L, LocalDate.of(2026, 4, 20)))
                .thenReturn(List.of(slot));
        when(bookingService.buildAiBookingDraft(101L, 1L, 88L, "Initial Consultation", "Sleep issue"))
                .thenReturn(AiBookingFormDraftVO.builder()
                        .customerId(101L)
                        .specialistId(1L)
                        .slotId(88L)
                        .topic("Initial Consultation")
                        .customerNotes("Sleep issue")
                        .availableTopics(List.of("Initial Consultation"))
                        .warnings(List.of())
                        .build());
        SpecialistDetailVO specialistDetail = new SpecialistDetailVO();
        specialistDetail.setId(1L);
        specialistDetail.setName("Dr. Sophie Zhao");
        specialistDetail.setConsultationFee(new BigDecimal("170.00"));
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialistDetail);

        AiBookingAutoSubmitResultVO result = tool.submitCurrentCustomerBooking(
                101L,
                1L,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                "Initial Consultation",
                "Sleep issue"
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isReadyToSubmit());
        assertEquals("Booking draft prepared. Please confirm submission in UI.", result.getMessage());
        assertNull(result.getBookingId());
        assertNull(result.getBookingStatus());
        assertEquals(88L, result.getSlotId());
        assertEquals("Dr. Sophie Zhao", result.getSpecialistName());
        assertEquals(0, new BigDecimal("170.00").compareTo(result.getConsultationFee()));
        assertEquals("Initial Consultation", result.getTopic());
    }

    @Test
    void shouldReturnAvailableSlotsWhenRequestedSlotNotFound() {
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        AiBookingSubmitTool tool = new AiBookingSubmitTool(specialistQueryService, bookingService);

        SpecialistAvailabilityVO slot = new SpecialistAvailabilityVO();
        slot.setId(89L);
        slot.setSlotDate(LocalDate.of(2026, 4, 20));
        slot.setStartTime(LocalTime.of(11, 0));
        slot.setEndTime(LocalTime.of(11, 30));
        slot.setStatus("AVAILABLE");

        when(specialistQueryService.listAvailability(1L, LocalDate.of(2026, 4, 20)))
                .thenReturn(List.of(slot));

        AiBookingAutoSubmitResultVO result = tool.submitCurrentCustomerBooking(
                101L,
                1L,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                "Initial Consultation",
                "Sleep issue"
        );

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertFalse(result.isReadyToSubmit());
        assertEquals("Requested slot is not available. Please choose one of availableSlots.", result.getMessage());
        assertNotNull(result.getAvailableSlots());
        assertEquals(1, result.getAvailableSlots().size());
        assertEquals(89L, result.getAvailableSlots().get(0).getSlotId());
    }
}
