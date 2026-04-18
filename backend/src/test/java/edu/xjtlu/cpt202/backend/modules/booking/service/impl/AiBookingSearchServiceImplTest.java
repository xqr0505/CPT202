package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 * @author QiranXiao
 * @since 2026/4/17
 */
@ExtendWith(MockitoExtension.class)
class AiBookingSearchServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private AiBookingSearchServiceImpl aiBookingSearchService;

    @AfterEach
    void tearDown() {
    }

    @Test
    void shouldSearchCurrentCustomerBookingsWithNormalizedFilters() {
        AiBookingSearchDTO queryDTO = new AiBookingSearchDTO();
        queryDTO.setExpertName("  Smith ");
        queryDTO.setCategoryName(" PSY ");
        queryDTO.setStatus("confirmed");

        List<AiBookingSearchItemVO> items = List.of(AiBookingSearchItemVO.builder().bookingId("1001").build());
        when(bookingMapper.selectAiBookingSearchList(
                eq(88L), any(AiBookingSearchDTO.class), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(items);
        when(bookingMapper.countAiBookingSearchList(
                eq(88L), any(AiBookingSearchDTO.class), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(1L);

        AiBookingSearchResultVO result = aiBookingSearchService.searchCustomerBookings(88L, queryDTO);

        assertNotNull(result);
        assertEquals(1L, result.getTotalMatched());
        assertEquals(1, result.getReturnedCount());
        assertEquals(1, result.getItems().size());

        ArgumentCaptor<AiBookingSearchDTO> queryCaptor = ArgumentCaptor.forClass(AiBookingSearchDTO.class);
        verify(bookingMapper).selectAiBookingSearchList(
                eq(88L), queryCaptor.capture(), any(), any(), any(LocalDateTime.class), eq(false), eq(false), eq(false), eq(10));
        AiBookingSearchDTO normalizedQuery = queryCaptor.getValue();
        assertEquals("Smith", normalizedQuery.getExpertName());
        assertEquals("PSY", normalizedQuery.getCategoryName());
        assertEquals("CONFIRMED", normalizedQuery.getStatus());
    }

    @Test
    void shouldPreferExplicitDateRangeOverTimeRangeType() {
        AiBookingSearchDTO queryDTO = new AiBookingSearchDTO();
        queryDTO.setStartDate(LocalDate.of(2026, 4, 1));
        queryDTO.setEndDate(LocalDate.of(2026, 4, 30));
        queryDTO.setTimeRangeType("UPCOMING");

        when(bookingMapper.selectAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(List.of());
        when(bookingMapper.countAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(0L);

        aiBookingSearchService.searchCustomerBookings(66L, queryDTO);

        verify(bookingMapper).selectAiBookingSearchList(
                eq(66L),
                any(AiBookingSearchDTO.class),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                any(LocalDateTime.class),
                eq(false),
                eq(false),
                eq(false),
                eq(10)
        );
    }

    @Test
    void shouldUseUpcomingTimeFilterAndAscendingSort() {
        AiBookingSearchDTO queryDTO = new AiBookingSearchDTO();
        queryDTO.setTimeRangeType("upcoming");

        when(bookingMapper.selectAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(List.of());
        when(bookingMapper.countAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(0L);

        aiBookingSearchService.searchCustomerBookings(12L, queryDTO);

        verify(bookingMapper).selectAiBookingSearchList(
                eq(12L), any(AiBookingSearchDTO.class), eq(null), eq(null), any(LocalDateTime.class), eq(true), eq(false), eq(true), eq(10));
    }

    @Test
    void shouldUseHistoryTimeFilter() {
        AiBookingSearchDTO queryDTO = new AiBookingSearchDTO();
        queryDTO.setTimeRangeType("history");

        when(bookingMapper.selectAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(List.of());
        when(bookingMapper.countAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(0L);

        aiBookingSearchService.searchCustomerBookings(15L, queryDTO);

        verify(bookingMapper).selectAiBookingSearchList(
                eq(15L), any(AiBookingSearchDTO.class), eq(null), eq(null), any(LocalDateTime.class), eq(false), eq(true), eq(false), eq(10));
    }

    @Test
    void shouldReturnEmptyResultWhenNoBookingsFound() {
        when(bookingMapper.selectAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(null);
        when(bookingMapper.countAiBookingSearchList(anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(null);

        AiBookingSearchResultVO result = aiBookingSearchService.searchCustomerBookings(21L, new AiBookingSearchDTO());

        assertEquals(0L, result.getTotalMatched());
        assertEquals(0, result.getReturnedCount());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void shouldRejectMissingCustomerId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiBookingSearchService.searchCustomerBookings(null, new AiBookingSearchDTO()));

        assertEquals(ResultCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
    }
}
