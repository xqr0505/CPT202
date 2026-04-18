package edu.xjtlu.cpt202.backend.modules.ai.tool;

import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.AiBookingSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBookingSearchToolTest {

    @Test
    void shouldDelegateToSearchServiceWithoutCustomerIdInput() {
        AiBookingSearchService aiBookingSearchService = mock(AiBookingSearchService.class);
        AiBookingSearchTool tool = new AiBookingSearchTool(aiBookingSearchService);
        AiBookingSearchResultVO expected = AiBookingSearchResultVO.builder()
                .totalMatched(1L)
                .returnedCount(1)
                .build();
        when(aiBookingSearchService.searchCustomerBookings(org.mockito.ArgumentMatchers.eq(32L), any(AiBookingSearchDTO.class)))
                .thenReturn(expected);

        AiBookingSearchResultVO actual = tool.searchCurrentCustomerBookings(
                32L,
                "Dr. Li",
                "Psychology",
                "CONFIRMED",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                "THIS_MONTH"
        );

        assertSame(expected, actual);

        ArgumentCaptor<AiBookingSearchDTO> captor = ArgumentCaptor.forClass(AiBookingSearchDTO.class);
        verify(aiBookingSearchService).searchCustomerBookings(org.mockito.ArgumentMatchers.eq(32L), captor.capture());
        AiBookingSearchDTO dto = captor.getValue();
        assertEquals("Dr. Li", dto.getExpertName());
        assertEquals("Psychology", dto.getCategoryName());
        assertEquals("CONFIRMED", dto.getStatus());
        assertEquals(LocalDate.of(2026, 4, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 30), dto.getEndDate());
        assertEquals("THIS_MONTH", dto.getTimeRangeType());
    }
}
