package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistQueryService specialistQueryService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    public void getUpcomingBookingsByCustomer_Success() {
        // Arrange
        Long customerId = 1L;
        int limit = 3;
        UpcomingBookingVO bookingVO = new UpcomingBookingVO();
        bookingVO.setId(1L);
        List<UpcomingBookingVO> mockResponse = Collections.singletonList(bookingVO);

        when(bookingMapper.selectUpcomingBookings(eq(customerId), anyString(), any(LocalDateTime.class), eq(limit)))
                .thenReturn(mockResponse);

        // Act
        List<UpcomingBookingVO> result = bookingService.getUpcomingBookingsByCustomer(customerId, limit);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
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

    @Test
    public void createBooking_Success() {
        Long customerId = 1L;
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic(" Career Planning ");
        createDTO.setCustomerNotes("Need help with internship planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(new BigDecimal("88.50"));

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(1);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingMapper.insert(bookingCaptor.capture())).thenAnswer(invocation -> {
            Booking booking = bookingCaptor.getValue();
            booking.setId(99L);
            return 1;
        });

        BookingCreateVO result = bookingService.createBooking(customerId, createDTO);

        assertNotNull(result);
        assertEquals(99L, result.getBookingId());
        assertEquals(BookingStatusEnum.PENDING.name(), result.getStatus());

        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(customerId, savedBooking.getCustomerId());
        assertEquals(1L, savedBooking.getSpecialistId());
        assertEquals(11L, savedBooking.getSlotId());
        assertEquals("Career Planning", savedBooking.getTopic());
        assertEquals(new BigDecimal("88.50"), savedBooking.getPrice());

        assertEquals(TimeSlotStatusEnum.BOOKED.name(), slot.getStatus());
        verify(timeSlotMapper).update(any(TimeSlot.class), any());
    }

    @Test
    public void createBooking_TimeSlotNotFound() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        when(timeSlotMapper.selectById(11L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("Time slot not found", exception.getMessage());
    }

    @Test
    public void createBooking_SlotBelongsToAnotherSpecialist() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(2L);

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Slot does not belong to the selected specialist", exception.getMessage());
    }

    @Test
    public void createBooking_TimeSlotAlreadyBooked() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Time slot already booked", exception.getMessage());
    }

    @Test
    public void createBooking_TopicNotAllowed() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Stress Management");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Topic is not available for this specialist", exception.getMessage());
    }

    @Test
    public void createBooking_ConcurrentUpdateFails() {
        BookingCreateDTO createDTO = new BookingCreateDTO();
        createDTO.setSpecialistId(1L);
        createDTO.setSlotId(11L);
        createDTO.setTopic("Career Planning");

        TimeSlot slot = new TimeSlot();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        SpecialistDetailVO specialist = new SpecialistDetailVO();
        specialist.setConsultationFee(BigDecimal.TEN);

        when(timeSlotMapper.selectById(11L)).thenReturn(slot);
        when(specialistQueryService.getSpecialistDetail(1L)).thenReturn(specialist);
        when(bookingMapper.insert(any(Booking.class))).thenReturn(1);
        when(timeSlotMapper.update(any(TimeSlot.class), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                bookingService.createBooking(1L, createDTO));

        assertEquals(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Time slot already booked", exception.getMessage());
    }
}

