package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @date 2026/4/1
 *
 */
@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    public void getUpcomingBookingsByCustomer_Success() {
        // Arrange
        Long customerId = 1L;
        int limit = 3;
        List<UpcomingBookingVO> mockResponse = List.of(
                UpcomingBookingVO.builder()
                        .id(1L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(LocalDateTime.of(2026, 4, 3, 10, 0, 0))
                        .today(true)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(2L)
                        .specialistName("Dr. Adam Smith")
                        .serviceName("Career Planning")
                        .startTime(LocalDateTime.of(2026, 4, 4, 14, 0, 0))
                        .today(false)
                        .status("CONFIRMED")
                        .build(),
                UpcomingBookingVO.builder()
                        .id(3L)
                        .specialistName("Schedule Dev Specialist")
                        .serviceName("Counseling")
                        .startTime(LocalDateTime.of(2026, 4, 5, 9, 0, 0))
                        .today(false)
                        .status("CONFIRMED")
                        .build()
        );
        when(bookingMapper.selectUpcomingBookings(eq(customerId), anyString(), any(LocalDateTime.class), eq(limit)))
                .thenReturn(mockResponse);

        // Act
        List<UpcomingBookingVO> result = bookingService.getUpcomingBookingsByCustomer(customerId, limit);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Schedule Dev Specialist", result.get(0).getSpecialistName());
        assertEquals("Counseling", result.get(0).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 3, 10, 0, 0), result.get(0).getStartTime());
        assertTrue(result.get(0).getToday());
        assertEquals("CONFIRMED", result.get(0).getStatus());
        assertEquals("Dr. Adam Smith", result.get(1).getSpecialistName());
        assertEquals("Career Planning", result.get(1).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 4, 14, 0, 0), result.get(1).getStartTime());
        assertFalse(result.get(1).getToday());
        assertEquals("CONFIRMED", result.get(1).getStatus());
        assertEquals("Schedule Dev Specialist", result.get(2).getSpecialistName());
        assertEquals("Counseling", result.get(2).getServiceName());
        assertEquals(LocalDateTime.of(2026, 4, 5, 9, 0, 0), result.get(2).getStartTime());
        assertFalse(result.get(2).getToday());
        assertEquals("CONFIRMED", result.get(2).getStatus());
    }

    @Test
    public void listBookings_Success() {
        // Arrange
        Long customerId = 1L;
        BookingHistoryQueryDTO queryDTO = new BookingHistoryQueryDTO();
        queryDTO.setPageNo(1);
        queryDTO.setPageSize(10);

        IPage<BookingHistoryListVO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(1);
        BookingHistoryListVO vo = new BookingHistoryListVO();
        mockPage.setRecords(Collections.singletonList(vo));

        when(bookingMapper.listBookings(any(Page.class), eq(queryDTO), eq(customerId)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookingHistoryListVO> result = bookingService.listBookings(customerId, queryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    public void getBookingDetail_Success() {
        // Arrange
        Long bookingId = 1L;
        Long customerId = 1L;
        BookingDetailVO mockDetail = new BookingDetailVO();
        mockDetail.setId(bookingId);

        when(bookingMapper.getBookingDetail(bookingId, customerId))
                .thenReturn(mockDetail);

        // Act
        BookingDetailVO result = bookingService.getBookingDetail(bookingId, customerId);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.getId());
    }

    @Test
    public void getBookingDetail_NotFound() {
        // Arrange
        Long bookingId = 1L;
        Long customerId = 1L;

        when(bookingMapper.getBookingDetail(bookingId, customerId))
                .thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookingService.getBookingDetail(bookingId, customerId);
        });
        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }
}
