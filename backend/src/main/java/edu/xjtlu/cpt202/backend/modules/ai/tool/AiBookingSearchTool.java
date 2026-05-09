package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.AiBookingSearchService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * AI tool for searching the current customer's bookings.
 * @author QiranXiao
 * @since 2026/4/17
 */
@Component
public class AiBookingSearchTool {

    private final AiBookingSearchService aiBookingSearchService;

    public AiBookingSearchTool(AiBookingSearchService aiBookingSearchService) {
        this.aiBookingSearchService = aiBookingSearchService;
    }

    @Tool("""
            Search the current logged-in customer's bookings. This tool is read-only and only returns the current customer's own bookings.
            Use it when the user asks about their bookings, appointments, history, upcoming visits, a specific specialist, or a category.
            """)
    public AiBookingSearchResultVO searchCurrentCustomerBookings(
            @ToolMemoryId Long customerId,
            @P("Specialist name to match fuzzily, for example 'Smith'. Optional.") String expertName,
            @P("Category name to match fuzzily, for example 'Psychology'. Optional.") String categoryName,
            @P("Booking status such as PENDING, CONFIRMED, COMPLETED, or CANCELLED. Optional.") String status,
            @P("Explicit start date in ISO format yyyy-MM-dd. Optional.") LocalDate startDate,
            @P("Explicit end date in ISO format yyyy-MM-dd. Optional.") LocalDate endDate,
            @P("Quick time range: TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, UPCOMING, HISTORY. Ignored if explicit dates are provided. Optional.") String timeRangeType
    ) {
        AiBookingSearchDTO queryDTO = new AiBookingSearchDTO();
        queryDTO.setExpertName(expertName);
        queryDTO.setCategoryName(categoryName);
        queryDTO.setStatus(status);
        queryDTO.setStartDate(startDate);
        queryDTO.setEndDate(endDate);
        queryDTO.setTimeRangeType(timeRangeType);
        return aiBookingSearchService.searchCustomerBookings(customerId, queryDTO);
    }
}
