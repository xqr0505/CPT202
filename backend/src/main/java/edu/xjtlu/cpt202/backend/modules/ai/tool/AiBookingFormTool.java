package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.springframework.stereotype.Component;

@Component
public class AiBookingFormTool {

    private final BookingService bookingService;

    public AiBookingFormTool(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool("""
            Draft a booking form for the current logged-in customer.
            Use this tool when users ask you to help fill a booking request form, pick a valid topic, or clean notes before submission.
            """)
    public AiBookingFormDraftVO draftCurrentCustomerBookingForm(
            @ToolMemoryId Long customerId,
            @P("Specialist profile ID for the booking form. Optional.") Long specialistId,
            @P("Time slot ID for the booking form. Optional.") Long slotId,
            @P("Preferred booking topic from user input. Optional.") String preferredTopic,
            @P("Customer notes draft from user input. Optional.") String customerNotes
    ) {
        return bookingService.buildAiBookingDraft(customerId, specialistId, slotId, preferredTopic, customerNotes);
    }
}
