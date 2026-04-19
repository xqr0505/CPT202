package edu.xjtlu.cpt202.backend.modules.ai.tool;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBookingFormToolTest {

    @Test
    void shouldDelegateToBookingService() {
        BookingService bookingService = mock(BookingService.class);
        AiBookingFormTool tool = new AiBookingFormTool(bookingService);
        AiBookingFormDraftVO expected = AiBookingFormDraftVO.builder()
                .customerId(101L)
                .specialistId(11L)
                .slotId(301L)
                .topic("Initial Consultation")
                .customerNotes("Need help with anxiety.")
                .availableTopics(List.of("Initial Consultation", "Follow-up"))
                .warnings(List.of())
                .build();

        when(bookingService.buildAiBookingDraft(101L, 11L, 301L, "Initial Consultation", "Need help with anxiety."))
                .thenReturn(expected);

        AiBookingFormDraftVO actual = tool.draftCurrentCustomerBookingForm(
                101L,
                11L,
                301L,
                "Initial Consultation",
                "Need help with anxiety."
        );

        assertSame(expected, actual);
        verify(bookingService).buildAiBookingDraft(101L, 11L, 301L, "Initial Consultation", "Need help with anxiety.");
    }
}
